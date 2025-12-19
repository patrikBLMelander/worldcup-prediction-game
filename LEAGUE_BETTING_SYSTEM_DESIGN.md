# League Betting System Design

## Overview
Two betting types for private leagues: **Flat Stakes** and **Custom Stakes**.

---

## 1. Flat Stakes

### Configuration
- **Entry Price**: Set by league creator (e.g., $10, $50, etc.)
- **Payout Structure**: Two options:
  1. **Winner Takes All** - 100% to 1st place
  2. **Ranked Based** - Configurable percentages (e.g., 1st: 60%, 2nd: 30%, 3rd: 10%)

### Auto-Switch Suggestion
- **< 5 players**: Default to "Winner Takes All"
- **≥ 6 players**: Default to "Ranked Based" (but creator can override)

### UI Requirements
- **When joining**: Clear display of entry price
- **Leaderboard**: Live display of prize amounts next to each placement
  - Example: "1st Place: $300" or "1st: $180 (60%)"

### Tie Handling
- If multiple players tie for 1st place:
  - Split both 1st AND 2nd place prizes
  - Example: 2-way tie for 1st in a 60/30/10 split → each gets (60% + 30%) / 2 = 45%
- Same logic applies for ties at any rank

### Example Scenarios

**Scenario A: Winner Takes All (4 players, $10 entry)**
- Total pot: $40
- Winner gets: $40
- Others get: $0

**Scenario B: Ranked Based (6 players, $10 entry, 60/30/10 split)**
- Total pot: $60
- 1st place: $36 (60%)
- 2nd place: $18 (30%)
- 3rd place: $6 (10%)
- 4th-6th: $0

**Scenario C: Tie for 1st (6 players, $10 entry, 60/30/10 split, 2-way tie)**
- Total pot: $60
- 1st place (tied): $27 each (split 60% + 30% = 90% / 2)
- 2nd place: $0 (already included in tie split)
- 3rd place: $6 (10%)
- 4th-6th: $0

---

## 2. Custom Stakes

### Configuration
- **Entry**: Each player sets their own stake amount when joining
- **No limits**: Players can bet any amount (e.g., $1, $100, $1000)
- **Payout**: Always **Winner Takes All** with side pot logic

### Side Pot Logic
The winner can only win what they bet from each player. Remaining amounts form side pots for lower-ranked players.

### Example Scenarios

**Scenario A: 5 players with different stakes**
- Player 1: $1
- Player 2: $100
- Player 3: $100
- Player 4: $100
- Player 5: $1000
- **Total pot: $1,301**

**If Player 1 wins:**
- Gets $1 from each = $5
- Player 2-4: $0 (all went to Player 1)
- Player 5: Gets $900 back (they bet $1000, Player 1 only bet $1)

**If Player 5 wins:**
- Gets $1,301 (full pot)
- Others: $0

**If Player 2-4 wins (e.g., Player 2):**
- Gets $1 from Player 1 = $1
- Gets $100 from Player 3 = $100
- Gets $100 from Player 4 = $100
- Gets $100 from Player 5 = $100 (Player 5 bet $1000, but Player 2 only bet $100)
- **Total: $401**
- Player 5: Gets $900 back (remaining from their $1000 bet)

### UI Requirements
- **Terms display**: Clear explanation of side pot mechanics
- **When joining**: Input field for stake amount
- **Leaderboard**: Show each player's stake and potential winnings
- **Transparency**: All stakes visible to all league members

### Tie Handling
- Split the pot proportionally among tied players
- Side pot logic still applies (each tied player can only win what they bet from others)

---

## 3. League Rules (Both Types)

### Entry Window
- **League closes when it starts** (no late joins)
- Players must join before the league's start date

### Data Model Considerations

#### League Entity Additions
```java
// Betting type
enum BettingType {
    FLAT_STAKES,
    CUSTOM_STAKES
}

// For Flat Stakes
BigDecimal entryPrice;
PayoutStructure payoutStructure; // WINNER_TAKES_ALL or RANKED
Map<Integer, BigDecimal> rankedPercentages; // e.g., {1: 0.60, 2: 0.30, 3: 0.10}

// For Custom Stakes
// (stakes stored in LeagueMembership)
```

#### LeagueMembership Entity Additions
```java
// For Custom Stakes
BigDecimal stakeAmount; // null for Flat Stakes
Boolean stakePaid; // track payment status
```

---

## 4. Implementation Phases

### Phase 1: Flat Stakes (MVP)
1. Add `bettingType` field to League
2. Add `entryPrice` and `payoutStructure` for Flat Stakes
3. Add stake amount display on leaderboard
4. Calculate and display prizes
5. Handle ties in payout calculation

### Phase 2: Custom Stakes
1. Add stake input when joining league
2. Implement side pot calculation logic
3. Add terms/explanation UI
4. Display stakes and potential winnings on leaderboard
5. Handle ties with side pots

---

## 5. Edge Cases & Considerations

### Flat Stakes
- What if only 1 player joins? (refund or minimum players required?)
- What if payout percentages don't add up to 100%? (validation needed)
- What if ranked payout has more winners than payout slots? (e.g., 3-way tie for 1st in a 60/30/10 split)

### Custom Stakes
- What if someone bets $0? (should this be allowed?)
- What if all players bet the same amount? (becomes equivalent to Flat Stakes)
- How to handle partial payments? (stakePaid flag)
- What if someone joins but never pays? (disqualification logic)

### Both
- How to handle league cancellation/refunds?
- Real money vs. points/credits? (assume real money for now)
- Payment processing integration? (Stripe, PayPal, etc.)
- Legal/compliance considerations for real-money betting?

---

## 6. UI/UX Mockups (To Be Designed)

### League Creation
- [ ] Betting type selector (Flat Stakes / Custom Stakes)
- [ ] For Flat: Entry price input, payout structure selector, percentage inputs for ranked
- [ ] For Custom: Terms display, explanation of side pots

### League Join
- [ ] For Flat: Display entry price, confirm payment
- [ ] For Custom: Stake amount input, terms acceptance, confirm payment

### Leaderboard
- [ ] Show prize amounts next to rankings
- [ ] For Custom: Show each player's stake
- [ ] Live calculation as league progresses

### League Completion
- [ ] Final standings with prize distribution
- [ ] Payment processing/transfer
- [ ] Receipt/confirmation

---

## 7. Open Questions

1. **Payment Processing**: Which service? (Stripe recommended)
2. **Minimum Players**: Required for league to start?
3. **Refund Policy**: What if league is cancelled?
4. **Legal**: Is this legal in target markets? (gambling laws vary)
5. **Escrow**: Hold funds until league completion?
6. **Fees**: Platform fee? (e.g., 5% of pot)
7. **Currency**: Single currency or multi-currency support?

---

## Notes

- Start with **Flat Stakes** for MVP
- **Custom Stakes** adds significant complexity (side pot logic, variable stakes)
- Both systems need robust tie-handling
- Real money requires payment processing integration
- Consider legal/compliance requirements early

