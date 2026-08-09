# Plan Complet — PayHub, plateforme fintech distribuée autour de FinLedger

### Spring Boot · DDD · Clean/Hexagonal · Event-driven · Sagas · Observable · Recoverable
---

## 0. Vision et principes directeurs

**PayHub** est une plateforme d'apprentissage, séparée de FinLedger, qui simule un agrégateur de paiements appelé **EcoPay Network**. Elle onboarde des sous-marchands, accepte un paiement mobile money, le soumet au risque, applique un split marchand/frais, le règle sur un rail externe, le réconcilie, gère les remboursements et notifie le marchand.

Le but n'est pas de recréer Stripe ou Adyen. Le but est d'apprendre à concevoir, implémenter et opérer un système où les défaillances partielles sont normales sans perdre ni dupliquer un effet financier.

### 0.1 Frontière avec FinLedger — non négociable

FinLedger reste un dépôt, un produit et un bounded context indépendants. PayHub le consomme via son contrat REST/OpenAPI et ses événements outbox; il ne le fork pas, ne l'étend pas via SPI, et ne lit jamais ses tables directement.

| FinLedger possède | PayHub possède |
| --- | --- |
| comptes, écritures double-entry, holds/projections de solde, audit, taux, arithmétique de split (`%`, HALF_EVEN), politique de fee-reversal, refunds natifs, rail lifecycle (PENDING/SETTLED) | intention de paiement, cycle de vie externe, workflow, décision risque, conversation PSP réelle, reconciliation, onboarding marchand, projections UX, webhooks sortants |
| vérité monétaire et invariants comptables | orchestration et état opérationnel du paiement/marchand |

**Règle absolue :** aucun service PayHub ne stocke un solde autoritaire, ne recalcule un split, ni ne réimplémente une politique de reversal de frais. Toute conséquence monétaire passe par l'API idempotente de FinLedger — y compris pour les remboursements et les splits, qui ont chacun leur propre endpoint natif (`POST .../refunds`, `POST .../splits`), jamais un contournement via `journal-entries`.

**Frontière d'intégration** (confirmée par lecture directe du code FinLedger, ADR-005/006/009) : FinLedger expose un **module rails de première classe** (`POST .../rails/payments`, `POST .../rails/payments/{ref}/settle`, `POST .../rails/webhooks/settlement`), mais **le bean in-box `ManualRailAdapter` ne parle à aucun PSP réel** — c'est un adapter de référencement local (`manual-{uuid}`, pas de réseau). PayHub reste propriétaire de la vraie conversation PSP (Rail Adapter). **Seul l'Orchestrator** appelle FinLedger (`rails/payments`, `.../settle`, `.../splits`, `.../refunds`) via son `LedgerPort` — et seulement **après** acceptation PSP du traitement (ordre verrouillé §4.1). Jamais de webhook PSP pointé directement sur FinLedger (contrat HMAC incompatible avec un PSP mobile money réel).

### 0.2 Principes d'architecture

1. **Un seul scénario complet avant toute généralisation.** Une devise, un rail sandbox, un type de paiement, un flux capture/refund et un agrégateur hiérarchique.
2. **DDD, microservices dès le départ.** Chaque service correspond à un bounded context, possède son modèle et ses données, et communique par contrat. *(Décision tranchée : PayHub démarre directement en 10 services indépendants — voir §19 pour l'ADR correspondant. Le principe "monolithe d'abord" envisagé en v1 de ce plan est abandonné au profit d'un apprentissage direct des frontières inter-services, objectif prioritaire du projet.)*
3. **Jamais de base partagée.** Chaque service possède sa propre base/schéma; aucun accès direct à la base d'un autre service ni à celle de FinLedger.
4. **ACID local, BASE entre services.** Une transaction PostgreSQL locale protège l'état et l'outbox; entre services, saga, compensation, inbox et réconciliation remplacent 2PC/XA.
5. **At-least-once assumé.** La livraison est au moins une fois; l'effet métier est effectivement unique grâce aux idempotency keys, contraintes uniques et déduplication.
6. **État ambigu protégé.** Un timeout du rail ne signifie jamais « paiement échoué » : l'état devient `RECONCILIATION_REQUIRED` jusqu'à preuve externe.
7. **Observabilité, sécurité et recovery sont des fonctionnalités.** Aucun flux n'est terminé sans trace, métriques, runbook et procédure de replay.
8. **Complexité progressive.** Kafka, Temporal, Redis et Kubernetes sont introduits pour résoudre un problème démontré (voir le roadmap §17 pour le ticket exact qui introduit chacun) — jamais avant.
9. **Ne jamais dupliquer une capacité que FinLedger possède déjà.** Avant de concevoir un mécanisme PayHub (split, refund, hold, rail), vérifier d'abord si FinLedger l'expose nativement. Cette règle a directement corrigé trois erreurs de conception de la v1 de ce plan (voir historique en tête de document).

### 0.3 Cadrage carrière — pourquoi ce projet, maintenant

| Compétence évaluée en entretien | Preuve produite par PayHub |
| --- | --- |
| "Comment garantis-tu l'idempotence dans un système distribué ?" | §5 — idempotency records, inbox/outbox, `at-least-once + effet unique`, testé sous chaos (§14) |
| "Comment gères-tu une transaction qui touche plusieurs services ?" | §4 — sagas Temporal (paiement + refund) documentées, compensation explicite, rejet argumenté de 2PC/XA |
| "Comment raisonnes-tu sur CAP/PACELC dans un vrai système ?" | §3.3 — tableau de choix par composant |
| "Décris un incident que tu as géré et comment tu l'as diagnostiqué." | §12/§14 — game days chaos codifiés |
| "Comment structures-tu les bounded contexts d'un système de paiement ?" | §1.2/§1.3 — context map et langage ubiquitaire |
| "Quand consommes-tu une capacité existante plutôt que d'en réinventer une ?" | §0.1/§0.2.9 — la découverte du split/refund/rails natifs de FinLedger et la correction de design qui a suivi |

---

## 1. Scénario de référence et modèle métier

### 1.1 Le flux EcoPay Network

Un sous-marchand d'EcoPay (onboardé et actif) crée un paiement de 100 USD (devise v1 alignée sur le sandbox FinLedger `aggregator`). PayHub évalue le risque, sélectionne une règle de split déjà provisionnée côté FinLedger, soumet l'opération au PSP mobile-money stub (**MmSandbox** — ne pas confondre avec le label FinLedger « Send Tunnel », qui désigne le *sous-marchand* seedé), et **seulement après acceptation PSP** crée le PENDING FinLedger puis confirme le settlement (`.../settle`), déclenche le split, réconcilie un relevé rail, gère un éventuel remboursement et notifie le marchand.

### 1.2 Bounded contexts

| Bounded context | Agrégat principal | Responsabilité | Données détenues |
| --- | --- | --- | --- |
| Merchant | `Merchant` | onboarding, statut, tier, règle de split assignée ; provisioning FinLedger `SUB_MERCHANT` | statut, tier, `assignedRuleSetKey`, `finLedgerTenantId`, refs wallets |
| Payment Orchestrator | `Payment` | intention, état, saga paiement + saga refund, commandes métier | paiement, refund, idempotence, outbox |
| Risk | `RiskAssessment` | règles, limites, review manuelle | décision, raisons, versions de règles |
| Rail Adapter | `RailOperation` | conversation PSP réelle uniquement — jamais d'appel FinLedger | opération fournisseur, attempts, raw payload chiffré |
| Reconciliation | `ReconciliationRun`, `Break` | rapprochement statement/rail/ledger et résolution | lignes importées, breaks, décisions opérateur |
| Reporting | `PaymentView` | projections CQRS et recherche | tables dérivées, offsets, staleness |
| Notification | `WebhookDelivery` | livraison sortante et retry | endpoint, tentatives, reçu, idempotence |
| Merchant BFF | — | API orientée intégrateur/marchand | aucune vérité métier; cache optionnel |
| Ops BFF | — | revue risque, breaks, DLQ, approbation marchand, refund admin | aucune vérité métier; vues composées |
| FinLedger | `JournalEntry` | vérité financière, split, refund policy, rail lifecycle comptable | propriété exclusive de FinLedger |

### 1.3 Langage ubiquitaire

- **Payment intent** : demande client idempotente, pas encore un mouvement financier final.
- **Rail instruction (PENDING)** : représentation comptable FinLedger de "en attente de règlement" créée par `initiate` **seulement après** acceptation PSP du traitement — remplace la notion de hold pour le paiement entrant v1 (voir §9.1).
- **Capture/settlement** : confirmation finale que le rail a exécuté l'opération, actée côté FinLedger via `.../settle` (Orchestrator uniquement).
- **Rail operation** : tentative idempotente sur le PSP, identifiée par une référence fournisseur, possédée par le Rail Adapter ; le résultat (accepté / rejeté / ambigu) remonte à l'Orchestrator.
- **Break** : divergence explicite entre système interne et preuve externe, à résoudre et auditer.
- **Compensation** : nouvelle commande métier (release, reversal, refund), jamais une suppression.
- **Refund** : commande native FinLedger (`POST .../refunds`) liée à l'écriture originale via `reversesEntryId`, jamais une écriture manuelle reconstruite côté PayHub.

### 1.4 États du paiement (corrigé)

```text
CREATED
  -> RISK_PENDING
       -> RISK_APPROVED
       -> RISK_REJECTED                 (terminal)
       -> RISK_REVIEW -> RISK_APPROVED | RISK_REJECTED   (décision humaine, signal Temporal)

RISK_APPROVED
  -> RAIL_SUBMITTED            (SubmitToPSP — aucun appel FinLedger tant que le PSP n'a pas accepté)
       -> FAILED_FINAL         (rejet net PSP ; FinLedger jamais touché)
       -> RECONCILIATION_REQUIRED       (timeout/ambigu AVANT acceptation PSP ; pas de PENDING ledger)
            -> SETTLED         (preuve tardive d'exécution → Orchestrator initiate + settle)
            -> FAILED_FINAL    (preuve tardive de non-exécution / rejet)
       -> SETTLEMENT_PENDING   (PSP a accepté le traitement → Orchestrator InitiateRailPayment = PENDING)
            -> SETTLED         (Orchestrator ConfirmSettlement = .../settle)
            -> RECONCILIATION_REQUIRED  (timeout webhook APRÈS PENDING)
                 -> SETTLED           (preuve tardive → settle)
                 -> FAILED_FINAL      (Break résolu par "request reversal" — nettoie le PENDING)

SETTLED
  -> REFUND_REQUESTED
       -> REFUND_RAIL_SUBMITTED
            -> REFUND_SETTLEMENT_PENDING -> REFUND_SETTLED
            -> REFUND_RECONCILIATION_REQUIRED -> REFUND_SETTLED | REFUND_FAILED_FINAL
            -> REFUND_FAILED_FINAL     (rail rejette net le refund ; ledger refund non appelé)
```

**Ordre verrouillé (paiement)** : PSP d'abord, FinLedger ensuite. `RAIL_SUBMITTED` ne crée aucun PENDING. Le PENDING n'existe qu'en `SETTLEMENT_PENDING` (PSP a accepté le traitement, settlement final encore attendu). Un rejet net immédiat n'a donc rien à compenser côté ledger. Le nettoyage d'un PENDING orphelin passe par Reconciliation (`request reversal`), jamais par un inventé `cancel` FinLedger.

**Autres corrections** : `PROCESSING` / `NOTIFIED` supprimés ; `RISK_REVIEW` ajouté ; sous-arbre `REFUND_*` ; livraison webhook hors de `Payment` (agrégat `WebhookDelivery` uniquement).

Les transitions sont validées par l'agrégat `Payment`. Les événements externes ne modifient pas directement la base : ils déclenchent une commande idempotente qui vérifie la transition.

---

## 2. Architecture Clean / Hexagonale et structure du dépôt

### 2.0 Topologie globale — vue d'ensemble

```text
                                        ┌────────────────────┐
                                        │     API Gateway      │
                                        │  TLS · OIDC/JWT ·     │
                                        │  rate limit · WAF     │
                                        └──────────┬────────────┘
                                                    │
                ┌────────────────────────────────────┼─────────────────────────────────────┐
                │                                    │                                     │
        ┌───────▼────────┐                  ┌────────▼─────────┐                 ┌────────▼────────┐
        │  Merchant BFF   │                  │ Payment           │                 │   Ops BFF        │
        │  aucune vérité  │                  │ Orchestrator      │                 │  risk review,    │
        │  métier         │                  │ Temporal workflows │                 │  breaks, DLQ,    │
        └────────┬────────┘                  │ (Payment + Refund) │                 │  merchant approve,│
                 │                            └──┬───┬───┬───┬───┘                 │  refund admin     │
                 │                               │   │   │   │                     └────────┬─────────┘
     ┌───────────▼──────────┐         ┌──────────▼┐ ┌▼──────────┐ ┌▼───────────┐             │
     │  Reporting Service    │         │  Merchant  │ │  Risk     │ │ Rail       │   ┌─────────▼─────────┐
     │  (CQRS read model,    │         │  Service   │ │  Service  │ │ Adapter    │   │  Reconciliation     │
     │  Redis cache-aside)   │         └────────────┘ └───────────┘ │ Service    │   │  Service             │
     └───────────▲───────────┘                                      └─────┬──────┘   │  (choreography,      │
                 │                                          PSP mobile money │        │  advisory lock)      │
                 │                                          MmSandbox     │        └──────────▲───────────┘
                 │                                          (sandbox externe)  │                   │
                 │                                                             │                   │
        ┌────────┴─────────────────────────────────────────────────────────────┴───────────────────┴───┐
        │                              Kafka (event backbone)                                          │
        │  payment.lifecycle.v1 · ledger.journal-entry.v1 · rail.operation.v1 · *.retry.* / *.dlq       │
        └────────┬──────────────────────────────────────────────────────────────────────────▲───────────┘
                 │ CDC (Debezium, lit le WAL Postgres)                                        │
                 │                                                                             │
        ┌────────▼────────┐     REST (Orchestrator LedgerPort ONLY)                  ┌──────────┴──────────┐
        │   FinLedger      │◀── rails/payments, /settle, /splits, /refunds ──────────│  (pas Notification)  │
        │  (core, jamais    │     Merchant : AccountProvisioningPort                  │                      │
        │  forké) — outbox  │     (SUB_MERCHANT tenant + wallets)                    │  Notification        │
        │  → Debezium       │──CDC──▶ ledger.journal-entry.v1 (Reporting, Reconcile) │  ← payment.lifecycle │
        └──────────────────┘                                                          │    .v1 (Orchestrator)│
                                                                                        │  livre via RabbitMQ  │
                                                                                        └────────────────────────┘

  Ownership FinLedger : Orchestrator = rails/settle/splits/refunds ; Merchant = SUB_MERCHANT tenant + wallets ;
  Rail Adapter = PSP uniquement (zéro appel FinLedger). Notification ne lit jamais l'outbox FinLedger.

  Cross-cutting : Service discovery (Compose DNS → K8s CoreDNS) · Distributed lock (Postgres advisory / K8s Lease)
  · Observabilité OTel (traceparent Gateway → ... → consumer) · Circuit breaker / bulkhead / retry par dépendance
```

**Corrections** : Notification consomme uniquement `payment.lifecycle.v1` (Orchestrator). Rail Adapter ne parle qu'au PSP. Tous les appels rails/settle/splits/refunds partent de l'Orchestrator. `merchant-service` a sa propre ACL FinLedger (provisioning), distincte.

### 2.1 Dépôt et modules

```
payhub-platform/
├── docs/
│   ├── adr/
│   ├── context-map.md
│   ├── event-catalog.md
│   ├── OPEN_QUESTIONS.md
│   ├── runbooks/
│   └── threat-model.md
├── build-conventions/          # ArchUnit de base, Checkstyle/Spotless, fixtures de test partagées
├── services/
│   ├── config-server/           # infra — Spring Cloud Config (pas un bounded context)
│   ├── gateway/
│   ├── merchant-bff/
│   ├── ops-bff/
│   ├── merchant-service/        # onboarding, statut, tier, règle de split assignée
│   ├── payment-orchestrator/     # Payment + Refund workflows
│   ├── risk-service/
│   ├── rail-adapter-service/
│   ├── reconciliation-service/
│   ├── reporting-service/
│   └── notification-service/
├── contracts/
├── platform/
│   ├── ports.md                 # table ports host / noms logiques (registry DNS)
│   ├── config/                  # YAML multi-profil servi par config-server
│   ├── compose/
│   ├── kafka/
│   ├── k8s/
│   ├── observability/
│   └── chaos/
└── finledger/                   # image digest/config + copie de référence d'INTEGRATION_GUIDE.md
```

### 2.2 Ports structurants

| Service | Port sortant | Adapter initial |
| --- | --- | --- |
| Orchestrator | `LedgerPort` | **seul** client paiement/refund FinLedger (`rails/payments`, `.../settle`, `.../splits`, `.../refunds`) |
| Orchestrator | `RailPort` | REST interne vers `rail-adapter-service` (soumission PSP + statut) — jamais FinLedger |
| Orchestrator | `WorkflowPort` | Temporal (`PaymentCaptureWorkflow`, `RefundWorkflow`) |
| Orchestrator | `RiskPort` | REST interne / adapter in-memory pour tests |
| Orchestrator | `MerchantPort` | REST interne vers `merchant-service` (`merchantId → assignedRuleSetKey`) |
| Merchant Service | `AccountProvisioningPort` | client REST FinLedger **séparé** — à l'activation : créer tenant `SUB_MERCHANT` + wallets (2ᵉ ACL ; aligné sandbox aggregator) |
| Rail Adapter | `RailProviderPort` | PSP sandbox MmSandbox uniquement — **zéro** dépendance FinLedger |
| Reconciliation | `StatementSourcePort` | fichiers CSV sandbox, ensuite SFTP/API |
| Notification | `WebhookTransportPort` | HTTP signé HMAC |
| Reporting | `ProjectionStorePort` | PostgreSQL puis Redis cache-aside |

### 2.3 Pourquoi pas un « shared domain »

Seuls les contrats techniques étroits (trace context, event envelope, test fixtures) peuvent être partagés. Chaque service qui parle à FinLedger a **sa propre** anti-corruption layer (`FinLedgerClient` de l'Orchestrator via `LedgerPort` ; client Merchant via `AccountProvisioningPort` pour tenant+wallets). Pas de module « shared ledger SDK » métier partagé entre services.

---

## 3. Contrats, données et cohérence

### 3.1 Ownership et transactions

Chaque service possède une base PostgreSQL ou un schéma strictement isolé. Une commande métier réalise dans **une transaction locale** : validation de l'agrégat, mutation de l'état, insertion de l'idempotency record et insertion outbox. Aucun dual write, aucune XA.

### 3.2 CQRS et Event Sourcing

- **CQRS léger :** Payment Orchestrator, Merchant Service et FinLedger ont des write models autoritaires; Reporting construit des projections à partir de Kafka et expose un `asOf`/consumer lag.
- **Pas d'event sourcing global.** Event sourcing ciblé autorisé en POC sur la timeline `Break` ou l'historique workflow, sous ADR.

### 3.3 CAP, PACELC, ACID et BASE

| Élément | Choix pendant partition (CAP) | Choix hors partition (PACELC) |
| --- | --- | --- |
| FinLedger | refuse une écriture si ses préconditions ne sont pas vérifiables | accepte la latence d'une transaction cohérente |
| Payment Orchestrator | persiste localement l'état atteint; ne conclut jamais à la place du rail | privilégie workflow durable plutôt qu'appel synchrone long |
| Rail Adapter | conserve l'ambiguïté et interroge/réconcilie | préfère idempotence et confirmation à une réponse optimiste |
| Reporting/BFF lecture | sert dernière vue disponible avec staleness | privilégie faible latence/cache, jamais vérité financière |

---

## 4. Sagas et transactions distribuées

### 4.1 Saga de paiement — `PaymentCaptureWorkflow`

Le workflow Temporal démarre dès `CreatePayment` (persisté `CREATED`). `EvaluateRisk` borne la réponse HTTP ; si `RISK_REVIEW`, le workflow attend un signal Ops. La suite (rail → ledger) est asynchrone.

```text
CreatePayment                                              -> CREATED  (démarre PaymentCaptureWorkflow)
EvaluateRisk (SYNCHRONE — borne la réponse HTTP)            -> RISK_PENDING -> RISK_APPROVED | RISK_REJECTED | RISK_REVIEW
[réponse HTTP synchrone : RISK_APPROVED / RISK_REJECTED / RISK_REVIEW]
--- asynchrone après RISK_APPROVED (ou signal review → APPROVED) ---
SelectSplitRuleKey (MerchantPort : merchantId -> assignedRuleSetKey — pas de calcul)
SubmitToPSP (RailPort → Rail Adapter → MmSandbox)        -> RAIL_SUBMITTED
  | rejet net PSP                                         -> FAILED_FINAL          (FinLedger intact)
  | timeout / ambigu AVANT acceptation                      -> RECONCILIATION_REQUIRED (pas de PENDING)
InitiateRailPayment (Orchestrator LedgerPort ONLY
  POST .../rails/payments) — après acceptation PSP           -> SETTLEMENT_PENDING   (PENDING créé)
  | initiate échoue après acceptation PSP                   -> retry idempotent, jamais abandonné (même règle que refund §4.3)
AwaitFinalRailProof (webhook/poll via Rail Adapter)
ConfirmSettlement (Orchestrator LedgerPort .../settle)       -> SETTLED
  | timeout webhook APRÈS PENDING                           -> RECONCILIATION_REQUIRED -> SETTLED | FAILED_FINAL
                                                              (FAILED_FINAL + Break "request reversal" nettoie le PENDING)
ApplySplit (Orchestrator LedgerPort .../splits, ruleSetKey)  -> (post-SETTLED ; pas d'état Payment séparé)
EmitPaymentSettled
DeliverWebhook (async, non bloquant — agrégat WebhookDelivery)
```

| Échec | Décision | Compensation |
| --- | --- | --- |
| Risk rejeté / review négative | terminal avant tout appel rail | aucune |
| Rail rejeté net (avant tout FinLedger) | `FAILED_FINAL` | aucune — FinLedger jamais touché |
| Rail timeout / ambigu **avant** acceptation | `RECONCILIATION_REQUIRED` | poll/statement ; si exécuté tardivement → initiate+settle ; sinon `FAILED_FINAL` |
| `initiate` échoue **après** acceptation PSP | retry idempotent, jamais abandonné | alerte si bloqué au-delà d'un seuil (argent PSP sans PENDING = incident) |
| Timeout webhook **après** PENDING | `RECONCILIATION_REQUIRED` | preuve → settle ; ou Break `request reversal` → `FAILED_FINAL` |
| `.../settle` échoue après preuve finale PSP | retry idempotent, jamais abandonné | alerte si bloqué |
| `.../splits` échoue après `SETTLED` | retry idempotent + alerte / Break ops si SLA dépassé | `Payment` reste `SETTLED` (pas de régression d'état) |
| Notification échoue | n'affecte pas le paiement | retry/DLQ delivery |

### 4.2 Orchestration vs choreography

- **Orchestration (Temporal) :** paiement et refund — ordre, timeouts, décisions humaines et compensations centraux.
- **Choreography :** Reporting, Notification, Reconciliation candidate réagissent à `PaymentSettled` de façon indépendante.
- **2PC/XA explicitement rejetée** — sagas et preuve de réconciliation, jamais de transaction distribuée.

### 4.3 Saga de remboursement — `RefundWorkflow`

FinLedger possède nativement la politique de fee-reversal, configurée **par tenant** via `PUT .../fee-config` (`feeReversalPolicy`), appliquée par `POST .../refunds`. PayHub **ne réimplémente aucune arithmétique de reversal** — il appelle l'endpoint natif avec le montant demandé.

```text
RefundRequested (initié par un opérateur via Ops BFF, v1)     -> REFUND_REQUESTED
  validation locale : refund demandé <= solde remboursable restant
  (le solde remboursable dépend de la policy active : total principal sous NO_REVERSE,
   total débit sous PRO_RATA — FinLedger rejette aussi si dépassé, double garde)
SubmitRefundToPSP (rail avant ledger — ordre verrouillé)       -> REFUND_RAIL_SUBMITTED
  succès rail confirmé                                        -> REFUND_SETTLEMENT_PENDING
FinLedger POST .../refunds (transactionReference, originalJournalEntryId,
  refundAmount, currencyCode, Idempotency-Key)                -> REFUND_SETTLED
  | ambigu/timeout côté rail                                   -> REFUND_RECONCILIATION_REQUIRED -> REFUND_SETTLED | REFUND_FAILED_FINAL
  | rail rejette net                                            -> REFUND_FAILED_FINAL
```

**Ordre verrouillé : rail d'abord, ledger ensuite.** Si le rail confirme le remboursement mais que l'appel `.../refunds` échoue, on retry idempotent jusqu'à succès (jamais abandonné) — inverser l'ordre obligerait à un chemin de compensation "reverse-du-refund" nettement plus complexe et évitable.

**Politique EcoPay v1 : `NO_REVERSE`** (les frais restent acquis à la plateforme même sur un remboursement partiel — comportement standard PSP). `PRO_RATA` (reversal proportionnel des frais) reste disponible comme simple bascule de configuration tenant côté FinLedger, sans aucun changement de code PayHub — documenté comme option, pas comme choix figé.

Remboursement **partiel supporté nativement** dès v1 (FinLedger applique la policy sur le `refundAmount` demandé, quel qu'il soit) — contrairement à la restriction "remboursement total uniquement" envisagée dans une version antérieure de ce plan, désormais levée puisque FinLedger gère déjà l'arithmétique proportionnelle.

---

## 5. Idempotence, outbox, inbox et CDC

*(inchangé vs v1 — voir détails d'implémentation : idempotency_record, inbox par consumer, outbox transactionnel, CDC via Debezium sur l'outbox FinLedger)*

- Idempotence API sur toute commande mutante (`Idempotency-Key` + hash canonique, écrit dans la même transaction locale que la mutation).
- Inbox avant action pour tout consumer Kafka/RabbitMQ.
- Outbox transactionnel, jamais de dual-write.
- CDC Debezium sur le WAL Postgres de FinLedger, jamais un plugin `EventPublisher` in-process (cohérent avec §0.1).

---

## 6. Kafka, RabbitMQ et gouvernance des événements

### 6.1 Topics de départ (corrigé — topics fantômes supprimés)

| Topic | Producteur | Clé | Consumers | Retention |
| --- | --- | --- | --- | --- |
| `payment.lifecycle.v1` | Orchestrator | `paymentId` | Reporting, Notification, Reconciliation | 30 jours |
| `ledger.journal-entry.v1` | FinLedger CDC | `journalEntryId` | Reporting, Reconciliation | 90 jours |
| `rail.operation.v1` | Rail Adapter | `railOperationId` | Orchestrator, Reconciliation | 30 jours |
| `*.retry.*` / `*.dlq` | consumers | clé d'origine | outil de replay | politique dédiée |

**Suppressions vs v1** : `tenant.events.v1` (aucun "control plane" PayHub réel — la création de tenant reste une opération `platform:admin` sur FinLedger, consommée via API, pas via topic) et `ledger.account-impact.v1` (Reporting dérive l'impact compte directement des postings de `ledger.journal-entry.v1`, sans second topic dédié tant que le besoin n'est pas prouvé — §0.2.8).

### 6.2/6.3 — inchangé vs v1 (opérations Kafka, RabbitMQ comme laboratoire webhook)

---

## 7. Résilience, protection et limites de charge

*(inchangé vs v1)* — budget de latence recalculé : la réponse synchrone `POST /payments` ne couvre plus que Gateway + BFF + Orchestrator + Risk (≈350–500ms sur un SLO de 2s), FinLedger étant désormais entièrement hors du chemin critique synchrone (voir §4.1).

---

## 8. Cache, projections et expérience de lecture

*(inchangé vs v1)*

---

## 9. Rails, risque, marchands et réconciliation

### 9.1 Rail Adapter — PSP only ; FinLedger = Orchestrator only

Le Rail Adapter est le **seul** endroit où apparaissent les API, erreurs, signatures et identifiants du PSP réel (MmSandbox, sandbox). **Il n'appelle jamais FinLedger.** Il expose un résultat canonique (`ACCEPTED` / `REJECTED` / `AMBIGUOUS`) à l'Orchestrator via `RailPort` / événements `rail.operation.v1`.

Séquence Orchestrator (ordre verrouillé — voir §4.1) :

1. `SubmitToPSP` via `RailPort` → Rail Adapter parle au sandbox.
2. **Seulement si** le PSP a accepté le traitement : Orchestrator `LedgerPort.initiate` → `POST .../rails/payments` (PENDING sur `RAIL_CLEARING`). Ceci remplace tout hold séparé v1 ; `SUSPENSE_HOLD` reste hors scope (voir `OPEN_QUESTIONS.md` Q7).
3. Sur preuve finale PSP : Orchestrator `LedgerPort.settle` → `POST .../rails/payments/{ref}/settle` (JWT PayHub) — **jamais** le webhook HMAC FinLedger comme cible PSP.
4. Aucun "success" Payment sans preuve corrélée du PSP. Un rejet net avant l'étape 2 ne touche pas FinLedger.

### 9.2 Risk — inchangé vs v1 (règles déterministes versionnées, décision `APPROVE`/`REJECT`/`REVIEW`, désormais reflétée par l'état `RISK_REVIEW` en §1.4)

### 9.3 Merchant — nouveau bounded context

- Agrégat `Merchant` : `id`, `status` (`PENDING_REVIEW` / `ACTIVE` / `SUSPENDED`), `tier`, `assignedRuleSetKey`, `finLedgerTenantId`, refs wallets FinLedger.
- À l'activation (`ACTIVE`), `AccountProvisioningPort` crée un tenant FinLedger **`SUB_MERCHANT`** (enfant de l'agrégateur EcoPay) **et** ses wallets — ACL FinLedger **distincte** de celle de l'Orchestrator (décision DS-001 / Q14 ; alignée sandbox `aggregator` / `…a2`).
- Approbation/rejet exposés côté Ops BFF (`POST /ops/merchants/{id}/approve|reject`).
- Consommé par le Payment Orchestrator (`MerchantPort`) pour résoudre `merchantId → assignedRuleSetKey` (et `finLedgerTenantId` pour les appels ledger) avant `SelectSplitRuleKey`.

### 9.4 Reconciliation

PayHub Reconciliation importe un statement rail idempotent, corrèle références, produit un `Break`. Actions opérateur auditables : `confirm`, `retry query`, `request reversal` (nettoie un PENDING orphelin après `RECONCILIATION_REQUIRED` → `FAILED_FINAL`), jamais de SQL direct sur FinLedger. Un seul run actif par tenant/rail (advisory lock / Lease).

**Distinct** de la reconciliation in-box FinLedger (ADR-009 : `rail_instruction` vs settlement report) — voir `docs/context-map.md`.

---

## 10. Edge, sécurité et multi-tenancy

*(endpoints Ops BFF étendus ; IdP local = Zitadel — ADR-007)*

- Issuer OIDC local : Zitadel (`http://localhost:8090` en profil `identity` / DevContainer) ;
  datastore IdP = CockroachDB single-node (**uniquement** Zitadel, jamais une DB métier PayHub).
- JWT PayHub : `RS256`/`ES256` ; claims minimaux `sub` + `tenant_id` (UUID) pour l'isolation.
- Gateway rejette toute requête API sans JWT valide **avant** routage vers un service métier.
- `POST /ops/merchants/{id}/approve` / `/reject`
- `POST /ops/payments/{id}/refunds` → déclenche `RefundWorkflow` (v1 : initié par un opérateur ; ouverture éventuelle côté Merchant BFF en self-service référencée dans `OPEN_QUESTIONS.md`, même commande Orchestrator, autz différente — jamais de duplication de workflow)
- `GET/POST /ops/reconciliation/...` → proxy thin vers `reconciliation-service`
- Endpoints breaks/DLQ déjà prévus, inchangés
- FinLedger conserve son propre issuer (`internal` sandbox) — distinct du JWT edge PayHub.
---

## 11–16. Service discovery/K8s, Observabilité, HA/DR, Tests, CI/CD, Documentation

*(inchangés vs v1 dans leur substance — voir la version précédente de ce document pour le détail complet de chaque section ; seules les références de service/topic ci-dessus ont changé)*

Ajout : `docs/OPEN_QUESTIONS.md` référencé depuis §16 comme registre vivant des points non tranchés (ex. self-service refund côté Merchant BFF, usage futur de `SUSPENSE_HOLD` pour les payouts, PRO_RATA vs NO_REVERSE si le produit évolue).

---

## 17. Roadmap de développement (renuméroté)

1. **DS-001 — Cadrage DDD :** event storming, context map (incl. Merchant), langage ubiquitaire, ownership, ADR CAP/PACELC. *Exit : context map + ADR relus et approuvés, aucun BC sans propriétaire.*
2. **DS-002 — Fondation dépôt :** skeleton hexagonal des 10 services, ArchUnit, Compose, CI, conventions contrats. *Exit : `./mvnw test` vert sur les 10 modules, ArchUnit actif, Compose démarre.*
3. **DS-003 — Intégration FinLedger :** image versionnée, `LedgerPort`/`FinLedgerClient` Orchestrator (smoke `rails/payments` sandbox ou endpoint de connectivité documenté — pas un contournement métier via `journal-entries`), propagation tenant/trace/idempotence. *Exit : un appel `LedgerPort` rejoué avec la même `Idempotency-Key` ne crée aucun second effet.*
4. **DS-004 — Merchant service :** agrégat `Merchant`, provisioning tenant FinLedger `SUB_MERCHANT` + wallets à l'activation, endpoints Ops BFF approve/reject. *Exit : un marchand `PENDING_REVIEW → ACTIVE` obtient un `finLedgerTenantId` `SUB_MERCHANT` et des wallets référencés.*
5. **DS-005 — Backbone events :** outbox FinLedger → Debezium → Kafka, Schema Registry, AsyncAPI, premier consumer inbox. *Exit : replay Kafka/CDC ne double aucun effet fonctionnel.*
6. **DS-006 — Paiement happy path (sans rail réel) :** `Payment` aggregate, idempotence API, Temporal, réponse sync = `RISK_APPROVED` ; RailPort stub/in-memory. *Exit : `CREATED` → `RISK_APPROVED` + workflow démarré ; pas d'exigence `SETTLED` (c'est DS-008).*
7. **DS-007 — Garanties de traitement :** inbox/outbox standardisées, retries, side effects idempotents. *Exit : rejouer un message dupliqué ne produit aucun second effet.*
8. **DS-008 — Rail et compensation paiement :** ordre PSP→initiate→settle, MmSandbox, timeout ambigu avant/après PENDING. *Exit : (a) rejet net PSP → `FAILED_FINAL` sans appel FinLedger ; (b) timeout simulé → `RECONCILIATION_REQUIRED` jamais `SETTLED` prématuré ; (c) happy path sandbox → `SETTLED`.*
9. **DS-009 — Refund :** `RefundWorkflow`, `POST .../refunds`, policy `NO_REVERSE` provisionnée. *Exit : un remboursement partiel respecte la policy FinLedger sans arithmétique côté PayHub.*
10. **DS-010 — Reconciliation :** import statement, breaks, lock/leadership, console Ops minimale. *Exit : un `Break` créé par divergence est résolu par une commande auditée, jamais une correction SQL directe.*
11. **DS-011 — Edge :** Gateway, OIDC/JWT, isolation tenant, rate limiting, Merchant/Ops BFF. *Exit : un appel sans JWT valide est rejeté avant d'atteindre un service métier.*
12. **DS-012 — Tracing :** OTel de bout en bout. *Exit : une trace unique traverse Gateway → Orchestrator → FinLedger → Rail Adapter → Kafka → consumer.*
13. **DS-013 — CQRS :** Reporting projection, staleness, Redis cache-aside. *Exit : une lecture Reporting expose son `asOf`/lag, jamais présentée comme à jour par défaut.*
14. **DS-014 — Notifications :** webhooks signés, retries, DLQ; POC RabbitMQ documenté. *Exit : une livraison webhook échouée finit en DLQ observable, jamais perdue silencieusement.*
15. **DS-015 — Résilience :** budgets, timeouts, retries, circuit breakers, bulkheads, shedding, backpressure. *Exit : un rail lent n'épuise pas le pool de connexions FinLedger (bulkhead prouvé sous charge).*
16. **DS-016 — Event operations :** retry topics, replay tool, quotas, rebalances. *Exit : un message poison est isolé sans bloquer les autres partitions.*
17. **DS-017 — Chaos/load :** injection de pannes, blast radius, capacity report. *Exit : aucun doublon financier détecté après une expérience de chaos codifiée.*
18. **DS-018 — Kubernetes/GitOps :** Services/DNS, policies, HPA/KEDA, PDB. *Exit : un pod tué en plein saga voit son workflow repris par un autre worker Temporal.*
19. **DS-019 — Data safety :** HA DB/Kafka, PITR, restore test, migrations expand/contract. *Exit : une restauration vérifie les données et la reprise CDC sans divergence.*
20. **DS-020 — SRE :** SLO/error budgets, alertes, runbooks. *Exit : chaque alerte pointe vers un runbook testé au moins une fois.*
21. **DS-021 — Consensus lab :** etcd/KRaft, leader failure, fencing token. *Exit : une bascule de leader observée et documentée, sans consensus fait-maison.*
22. **DS-022 — DR game day :** perte simulée de zone, RPO/RTO mesurés. *Exit : RPO/RTO réels rapportés avec écarts documentés.*
23. **DS-023 — Mesh POC :** seulement après ADR bénéfice/coût. *Exit : comparaison chiffrée mTLS applicatif vs mesh, décision documentée.*
24. **DS-024 — Capstone :** démo paiement + refund avec panne rail/Kafka, reprise, reconciliation, audit, revue d'architecture. *Exit : un reviewer suit une trace de bout en bout (paiement ET refund) et explique chaque choix.*

---

## 18. Inspirations et positionnement

*(inchangé vs v1)*

---

## 19. Décisions structurantes — ne pas dévier sans ADR

| Décision | Alternative écartée | Raison |
| --- | --- | --- |
| PayHub démarre en 10 microservices, pas en monolithe modulaire | Monolithe d'abord, extraction par preuve | Objectif d'apprentissage prioritaire : les frontières inter-services sont le sujet du projet, pas un risque à différer |
| Merchant est un bounded context dédié (`merchant-service`) | Données seedées statiques, pas de service | Onboarding/approbation marchand est une action d'opérateur réelle (Ops BFF) ; Angular hors v1 (Q12) |
| Merchant actif → tenant FinLedger `SUB_MERCHANT` + wallets | Compte seul sous le tenant EcoPay | Aligné sandbox FinLedger `aggregator` ; JWT `tenant_id` / isolation cohérents (Q14) |
| Conversation PSP = Rail Adapter only ; appels FinLedger rails/settle/splits/refunds = Orchestrator `LedgerPort` only | Rail Adapter appelle aussi FinLedger ; ou webhook PSP → HMAC FinLedger | Sépare anti-corruption PSP vs ledger ; HMAC FinLedger ≠ contrat mobile money ; `ManualRailAdapter` ne parle à aucun PSP |
| Ordre paiement : PSP d'abord, puis `initiate` (PENDING), puis `settle` | `initiate` avant le PSP (PENDING orphelin si rejet net) | Un rejet immédiat n'a rien à compenser ; pas besoin d'inventer un `cancel` FinLedger (Q9) ; PENDING seulement après acceptation traitement |
| Pas de hold séparé (`SUSPENSE_HOLD`) pour le paiement entrant v1 | Hold explicite en plus du PENDING rail | Le PENDING post-acceptation PSP suffit ; `SUSPENSE_HOLD` réservé à d'autres besoins (Q7) |
| Deux ACL FinLedger distinctes (Orchestrator paiement ; Merchant provisioning) | Un seul client partagé cross-services | Responsabilités différentes ; pas de shared domain ledger |
| Split : Orchestrator sélectionne un `ruleSetKey`, FinLedger calcule (`/splits`) | Calcul du split (95/3/2, arrondi) côté PayHub | FinLedger possède déjà `DeclarativeSplitPlanResolver` + `SplitPlanEvaluator` (HALF_EVEN) ; dupliquer l'arithmétique financière viole §0.1 |
| Refund : appel natif `POST .../refunds`, policy tenant FinLedger (`feeReversalPolicy`) | Réimplémenter NO_REVERSE/PRO_RATA côté PayHub | FinLedger (ADR-005) supporte déjà les deux policies nativement, configurables sans code PayHub |
| Ordre refund : rail d'abord, ledger ensuite | Ledger d'abord | Évite un chemin de compensation "reverse-du-refund" ; retry idempotent illimité en cas d'échec `.../refunds` post-confirmation rail est plus simple |
| `tenant.events.v1` et `ledger.account-impact.v1` supprimés du catalogue v1 | Les garder avec un producteur à définir plus tard | Aucun service PayHub réel ne les produit ; les réintroduire quand un vrai besoin/propriétaire existe (§0.2.8) |
| FinLedger est unique vérité monétaire | Balance miroir dans Orchestrator/Redis | Audit et cohérence financière |
| Temporal orchestre paiement/refund | Choreography intégrale | Timeouts et compensations explicites |
| Outbox + CDC + inbox | Dual write, consumer naïf | Perte/doublon contrôlés |
| Résilience dans le code avant mesh | Istio/Linkerd day 1 | Comprendre les mécanismes, éviter les retries doublés |
| Consensus opéré, non implémenté | Écrire Raft/Paxos | Valeur réaliste pour un architecte backend |
| IdP local = Zitadel (Go) + CockroachDB (état IdP seulement) | Keycloak sur Postgres ; Cockroach comme DB métier PayHub | Diversité d'écosystème + OIDC DevContainer ; Postgres reste la DB de chaque service PayHub (ADR-007) |

---

## 20. Résultat attendu

PayHub doit démontrer des compétences Senior/Staff : frontières DDD défendables (y compris la correction explicite de conception documentée dans ce plan), contrats versionnés, cohérence distribuée explicitée, pannes simulées, recovery mesuré, et — point désormais central après cette revue — la discipline de **consommer une capacité existante de FinLedger plutôt que de la réinventer**, avec la trace écrite du raisonnement qui a mené à chaque correction.