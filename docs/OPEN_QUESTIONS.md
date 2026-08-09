# Questions ouvertes et hypothèses en attente

Registre vivant. Chaque entrée reste ici jusqu'à résolution par une ADR ou une mise à jour
explicite de `PLAN_PAYHUB.md`. Ne jamais résoudre une entrée silencieusement dans le code
sans mettre à jour ce fichier.

## Résolues depuis la dernière revue (gardées pour trace, à titre d'historique)

- ~~Q1 — Mécanisme exact du hold FinLedger~~ → **Résolu.** Pas de hold séparé en v1 : le
  PENDING créé par `POST .../rails/payments` **après acceptation PSP** représente
  "en attente de règlement" (voir `PLAN_PAYHUB.md` §1.4, §4.1, §9.1, §19).
- ~~Q2 — Ownership FinLedger rails/*~~ → **Résolu : Orchestrator only.** Rail Adapter =
  PSP uniquement. Merchant garde une ACL séparée pour provisioning tenant+wallets (Q14).
- ~~Q2b — Trou PENDING si initiate-avant-PSP~~ → **Résolu par réordonnancement.** PSP
  d'abord ; `initiate` seulement après acceptation traitement ; rejet net = zéro
  compensation ledger ; PENDING orphelin nettoyé via Break `request reversal`.
- ~~Q3 — Remboursement partiel~~ → **Résolu, supporté nativement.** FinLedger applique sa
  `feeReversalPolicy` (`NO_REVERSE`/`PRO_RATA`) sur n'importe quel `refundAmount` via
  `POST .../refunds` — aucune restriction "remboursement total uniquement" (voir §4.3).
- ~~Q4 — Merchant onboarding hors scope~~ → **Inversé.** `merchant-service` est désormais
  un bounded context à part entière dès DS-004 (portail admin Angular prévu — hors roadmap
  DS-0xx v1, voir Q12).
- ~~Q5 — `tenant.events.v1` utile via Q4~~ → **Toujours retiré du catalogue v1.** Pas de
  topic control-plane. La création du tenant FinLedger `SUB_MERCHANT` est un effet de
  l'activation marchand (Q14), pas un événement Kafka PayHub.
- ~~Q14 — Merchant = tenant FinLedger `SUB_MERCHANT` ou compte seul~~ → **Résolu (DS-001).**
  `Merchant` actif → tenant FinLedger `SUB_MERCHANT` + wallets via
  `AccountProvisioningPort` (voir `PLAN_PAYHUB.md` §9.3, §19, `context-map.md`).

## Toujours ouvertes

### Q6 — Refund self-service (Merchant BFF) ou opérateur uniquement (Ops BFF) ?

v1 assume un remboursement initié par un opérateur via l'Ops BFF (cohérent avec le futur
portail admin Angular). Aucune preuve produit encore qu'un marchand doive pouvoir
déclencher lui-même un remboursement en self-service.

**À lever :** quand le portail Angular est construit, ou si un vrai besoin de self-service
apparaît. Le cas échéant : même commande Orchestrator (`RefundWorkflow`), autorisation
différente — jamais de duplication de workflow (voir `PLAN_PAYHUB.md` §10).

### Q7 — `SUSPENSE_HOLD` (ADR-006 de FinLedger) sera-t-il un jour nécessaire côté PayHub ?

Pas utilisé dans le happy path v1 (le PENDING rail suffit pour un paiement entrant). Un
besoin réel et différent existerait pour un payout ou une réservation de solde marchand
avant une opération conditionnelle.

**À lever :** si/quand un flux de payout ou de réservation marchande est ajouté au scénario.

### Q8 — `feeReversalPolicy` EcoPay restera-t-il `NO_REVERSE` ?

v1 = `NO_REVERSE` (comportement PSP standard, aucun code PayHub à changer si ça évolue —
bascule de configuration tenant côté FinLedger uniquement).

**Partiel (DS-009) :** Orchestrator provisionne `NO_REVERSE` via
`PUT .../fee-config` (`payhub.finledger.provision-fee-config=true`) ;
`LedgerPort.refund` passe `refundAmount` tel quel à `POST .../refunds`
(champs : `transactionReference`, `originalJournalEntryId`, `refundAmount`,
`currencyCode`) — zéro arithmétique de fee côté PayHub.

**À lever :** seulement si le produit décide explicitement de vouloir reverser les frais
sur remboursement — décision commerciale, pas technique.

### Q9 — Schéma exact des requêtes `POST .../splits` et `POST .../rails/payments`

**Chemins confirmés** (copie `finledger/openapi-paths.json`, FL-160) :
`…/rails/payments`, `…/rails/payments/{railReference}/settle`, `…/splits`, `…/refunds`,
`…/split-rules/{ruleSetKey}`, `…/fee-config`. Aucun endpoint `cancel` rail — cohérent
avec l'ordre PSP→initiate (§4.1).

**Partiel (DS-004) — tenant + accounts :** champs pris des DTOs FinLedger sources (pas
inventés) :
- `POST /api/v1/tenants` → `CreateTenantRequest` (`name`, `type`, `parentTenantId`, `id`)
  in `finledger/.../presentation/rest/tenant/TenantController.java` ; result
  `CreateTenantResult` (`tenantId`, `name`, `type`, `parentTenantId`)
- `POST /api/v1/tenants/{tenantId}/accounts` → `CreateAccountRequest` (`ownerRef`,
  `currencyCode`, `type`, `allowsOverdraft`) in
  `finledger/.../presentation/rest/account/LedgerAccountController.java` ; result
  `CreateLedgerAccountResult` (`accountId`, …)

**Partiel (DS-008) — settle :** `POST …/rails/payments/{railReference}/settle`
avec `Idempotency-Key` + Bearer JWT (corps vide côté PayHub ACL). Initiate reste
inchangé (`railCode`, amount, currency, clearing/counterparty account ids,
clientReference).

**Reste ouvert :** schéma JSON champ par champ pour `…/splits` avant DS-009
ApplySplit.

### Q10 — Où vit la table de lookup de `SelectSplitRuleKey` ?

**Résolu (DS-004).** `Merchant.assignedRuleSetKey` est persisté sur l'agrégat Merchant
(`merchant-service`, table `merchant`). Orchestrator `MerchantPort` consommation =
DS-006+.

### Q11 — Multi-devise en v1 ?

**v1 sandbox = USD** (aligné pack FinLedger `aggregator`). XAF / multi-devise = post-v1.

**À lever :** seulement si le scénario est volontairement élargi.

### Q12 — Portail admin Angular : ticket dédié ou hors roadmap DS-0xx ?

**Décision v1 :** hors roadmap DS-0xx / post-capstone. Ops BFF REST suffit pour
DS-004/009/010. Ne pas démarrer d'UI Angular pendant les tickets plateforme.

### Q13 — `initiate` échoue après acceptation PSP → retry forever ?

**Décision par défaut (en vigueur dans §4.1) :** retry idempotent, jamais abandonné,
alerte si SLA dépassé — même pattern que refund rail→ledger. FinLedger n'expose pas de
`cancel` rail (openapi-paths). Un refus métier définitif FinLedger (ex. compte inexistant)
devient un Break ops / intervention, pas un `FAILED_FINAL` silencieux qui oublierait
l'argent PSP.

**À lever :** seulement si DS-003 révèle un code d'erreur FinLedger qui impose un autre
chemin.

### Q16 — Schema Registry format (Avro vs JSON Schema) pour `ledger.journal-entry.v1` ?

**Résolu (DS-005 / ADR-004).** JSON Schema via Confluent Schema Registry in Compose;
consumers deserialize JSON without Avro codegen. Revisit only if a later ticket proves
Avro/compatibility tooling is required.

### Q15 — Nommage : « Send Tunnel »

Dans FinLedger, **Send Tunnel** = label du sous-marchand sandbox. Dans PayHub, le PSP
stub s'appelle désormais **MmSandbox** pour éviter la collision. Ne pas réintroduire
« Send Tunnel » comme nom de PSP.

## Comment utiliser ce registre

- Nouvelle question ouverte pendant un ticket DS-0xx → ajouter une entrée ici avant de
  deviner une réponse dans le code.
- Question résolue → déplacer le raisonnement dans une ADR (`docs/adr/`), mettre à jour
  `PLAN_PAYHUB.md` si ça touche une section normative, puis marquer l'entrée résolue ici
  (garder une trace, ne pas juste supprimer).