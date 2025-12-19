# Multi-Competition Support - Design Discussion

**Status:** Design Phase - Not Implemented  
**Date:** December 2024  
**Priority:** Future Enhancement

## Overview

This document outlines the design for supporting multiple football competitions (World Cup, Premier League, Champions League, etc.) in the prediction game, with separate leaderboards per competition and competition-specific friend leagues.

## Requirements

### Core Requirements

1. **Separate Leaderboards**: One global leaderboard per competition (WC, PL, CL, etc.)
2. **Multi-Competition Participation**: Users can compete in multiple competitions simultaneously
3. **Friend League Competition**: Each friend league is tied to exactly one competition
4. **Match Filtering**: Only show matches for selected/active competitions
5. **Default Behavior**: When joining a friend league, show that competition's matches by default
6. **Global Competition Toggle**: Option in profile to see all global competition matches

## Competition List

Based on Football-Data.org API support:

- **WC** - FIFA World Cup
- **CL** - UEFA Champions League
- **BL1** - Bundesliga
- **DED** - Eredivisie
- **BSA** - Campeonato Brasileiro Série A
- **PD** - Primera Division (La Liga)
- **FL1** - Ligue 1
- **ELC** - Championship
- **PPL** - Primeira Liga
- **EC** - European Championship
- **SA** - Serie A
- **PL** - Premier League

## Architecture Decisions

### 1. User Competition Preferences

**Decision:** Use separate `UserCompetition` entity (join table)

**Rationale:**
- More flexible than storing Set in User entity
- Allows per-competition preferences (notifications, etc.)
- Better for future features

**Structure:**
```java
@Entity
class UserCompetition {
    @ManyToOne User user;
    String competitionId; // "WC", "PL", "CL"
    LocalDateTime joinedAt;
    boolean notificationsEnabled; // Future use
}
```

### 2. Match Filtering Logic

**Default Behavior:**
- New users: Show all matches (or prompt to select)
- After joining friend league: Show that league's competition matches
- After selecting global competitions: Show those matches
- Profile checkbox: "Show all competitions" override

**Flow:**
1. Check if user has active friend leagues → show those competitions
2. Check user's selected global competitions → show those
3. If neither → show all (or prompt selection)

### 3. Friend League Creation

- **Competition selection is REQUIRED**
- Date range should align with competition schedule
- Only matches from that competition count toward the league

### 4. Global Leaderboard Structure

**Recommended:** Tabs at top of leaderboard page

```
[All] [WC] [PL] [CL] [BL1] ...
```

- "All" tab: Summary or combined view (TBD)
- Individual tabs: Competition-specific leaderboards
- Quick switching between competitions

### 5. Profile Settings UI

**Competition Preferences Section:**

```
Competitions I'm Following:
☑ World Cup (Global)
☑ Premier League (Global)
☐ Champions League
☐ Bundesliga
...

[Save Preferences]
```

**Controls:**
- Which matches appear in main matches view
- Which global leaderboards user participates in
- Future: Notification preferences per competition

### 6. Match Entity Changes

**Add fields:**
```java
@Column(name = "competition_id")
private String competitionId; // "WC", "PL", "CL", etc.

@Column(name = "competition_name")
private String competitionName; // "World Cup", "Premier League", etc.
```

### 7. Friend League Entity Changes

**Add field:**
```java
@Column(name = "competition_id", nullable = false)
private String competitionId; // Required - which competition this league is for
```

## User Flow Examples

### Scenario 1: New User
1. User registers
2. Sees all matches (or prompt to select competitions)
3. Can join friend leagues (auto-follows that competition)
4. Can manually select global competitions in profile

### Scenario 2: User Joins Friend League
1. Joins "My World Cup League" (competition: WC)
2. Matches view automatically filters to World Cup matches
3. Can still see other competitions via profile settings
4. Leaderboard shows WC global + friend league

### Scenario 3: User Selects Global Competitions
1. Goes to Profile → Settings
2. Checks "Premier League" and "Champions League"
3. Matches view shows PL + CL matches
4. Can view PL and CL global leaderboards

## Technical Considerations

### 1. Match Sync from API

**Current State:**
- Syncs one competition (hardcoded 2021 = Premier League)

**New Requirements:**
- Sync multiple competitions
- Challenge: API rate limits (10 req/min free tier)
- Solution: Queue/prioritize competitions, cache results

**Implementation:**
- Modify `FootballApiService` to accept competition ID parameter
- Update scheduler to sync multiple competitions
- Implement rate limiting/queuing

### 2. Leaderboard Calculation

**Current State:**
- One global leaderboard

**New Requirements:**
- Per-competition leaderboards
- Performance: Cache leaderboards, recalculate on match finish
- Query: Filter predictions by competition via `match.competitionId`

**Implementation:**
- Modify leaderboard queries to filter by competition
- Add caching layer for performance
- Update leaderboard calculation triggers

### 3. Migration Strategy

**Existing Data:**
- Existing leagues: Assign default competition (e.g., Premier League = "PL")
- Existing matches: Assign competition based on source
- Existing users: Start with all competitions visible, or prompt selection

**Migration Steps:**
1. Add `competitionId` to Match entity (nullable initially)
2. Backfill existing matches with default competition
3. Add `competitionId` to League entity (nullable initially)
4. Backfill existing leagues with default competition
5. Create UserCompetition entries for all users (all competitions active)
6. Make fields non-nullable after migration

### 4. Data Consistency

**Requirements:**
- All matches must have `competitionId`
- Friend league `competitionId` must exist
- Handle competition changes (e.g., season transitions)

**Validation:**
- Add validation on match creation
- Add validation on league creation
- Add database constraints

## Open Questions

### 1. Competition Data Source
- **Option A:** Hardcode list in application
- **Option B:** Fetch from API
- **Option C:** Admin-configurable in database

**Recommendation:** Option A initially (hardcoded), migrate to Option C later for flexibility

### 2. Match Display Priority
- If user has friend leagues + global selections, show all?
- Or prioritize friend league competitions?

**Recommendation:** Show all active competitions (friend leagues + global selections)

### 3. Leaderboard "All" View
- Combined points across competitions?
- Or just a summary/selector?

**Recommendation:** Summary view showing top users across all competitions (combined points)

### 4. Competition Switching
- Quick switcher in matches view?
- Or only via profile settings?

**Recommendation:** Quick switcher in matches view + profile settings for permanent preferences

### 5. Friend League Visibility
- Show competition badge on league card?
- Filter friend leagues by competition?

**Recommendation:** Show competition badge/icon on league cards, add filter option

## Recommended Implementation Approach

### Phase 1: Foundation
1. Create Competition entity/enum with codes, names, API IDs
2. Add `competitionId` to Match entity
3. Add `competitionId` to League entity
4. Migrate existing data

### Phase 2: User Preferences
1. Create UserCompetition join table
2. Add competition selection to profile
3. Implement match filtering based on preferences

### Phase 3: Multi-Competition Support
1. Update API sync to handle multiple competitions
2. Implement per-competition leaderboards
3. Update UI for competition selection and filtering

### Phase 4: Friend League Integration
1. Require competition selection when creating leagues
2. Auto-add competition to user preferences on join
3. Filter matches by league competition

### Phase 5: Polish
1. Add competition badges/icons
2. Improve UI for competition switching
3. Add competition filters to various views

## Complexity Assessment

- **Database Changes:** Medium (new fields, join tables)
- **Backend Logic:** Medium-High (filtering, multiple leaderboards)
- **Frontend:** Medium (new UI components, filtering)
- **Migration:** Low-Medium (data backfill)
- **API Integration:** Medium (multi-competition sync)

**Overall:** Medium complexity, but significant architectural improvement

## Benefits

1. **Better Comparisons:** Users compete on same matches
2. **Clearer Organization:** Separate leaderboards per competition
3. **More Relevant Leagues:** Private leagues tied to specific competitions
4. **Scalable:** Easy to add more competitions
5. **Better UX:** Users see only relevant matches

## Risks & Mitigations

### Risk 1: API Rate Limits
- **Mitigation:** Implement queuing, caching, and prioritization

### Risk 2: Performance with Many Competitions
- **Mitigation:** Cache leaderboards, optimize queries, pagination

### Risk 3: Data Migration Complexity
- **Mitigation:** Careful migration script, test on staging first

### Risk 4: User Confusion
- **Mitigation:** Clear UI, good defaults, helpful tooltips

## Next Steps (When Ready)

1. Finalize competition data structure
2. Design database schema changes
3. Create migration scripts
4. Update API service for multi-competition
5. Implement user preferences system
6. Update match filtering logic
7. Implement per-competition leaderboards
8. Update UI components
9. Test thoroughly
10. Deploy

## Notes

- Current API supports competitions shown in image
- API is accessible from localhost (just needs API key)
- Current implementation hardcoded to competition ID 2021 (Premier League)
- This is a significant architectural change - plan carefully


