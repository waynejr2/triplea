package games.strategy.triplea.odds.calculator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.ResourceList;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DummyPlayerTest {

  private final List<Unit> unitPool =
      List.of(
          mock(Unit.class), mock(Unit.class), mock(Unit.class), mock(Unit.class), mock(Unit.class));

  private final List<Unit> orderOfLosses =
      List.of(unitPool.get(2), unitPool.get(0), unitPool.get(1), unitPool.get(4), unitPool.get(3));

  private final List<Unit> damaged = List.of(unitPool.get(0), unitPool.get(2));
  private final List<Unit> killed = List.of(unitPool.get(1), unitPool.get(0));

  private final CasualtyList defaultCasualties = mock(CasualtyList.class);

  @BeforeEach
  void setUp() {
    when(defaultCasualties.getDamaged()).thenReturn(damaged);
    when(defaultCasualties.getKilled()).thenReturn(killed);
  }

  private void assertCommonCasualtyDetails(final CasualtyDetails details) {
    assertThat(details.getDamaged(), is(damaged));
    assertThat(details.getAutoCalculated(), is(false));
  }

  @Nested
  class OneLand {
    @BeforeEach
    void setUp() {
      for (final var unit : unitPool) {
        final var unitType = mock(UnitType.class);
        when(unit.getType()).thenReturn(unitType);
        final var unitAttachment = mock(UnitAttachment.class);
        when(unitType.getAttachment(any())).thenReturn(unitAttachment);
        when(unitAttachment.getIsAir()).thenReturn(!unit.equals(unitPool.get(0)));
        final var gameData = mock(GameData.class);
        when(unit.getData()).thenReturn(gameData);
        when(gameData.getResourceList()).thenReturn(mock(ResourceList.class));
        final var playerId = mock(PlayerId.class);
        when(unit.getOwner()).thenReturn(playerId);
      }
    }

    /**
     * This test checks if units are correctly selected keeping in mind that some units might not be
     * able to capture the target territory on their own. (Like air units for example.) This only
     * works if no orderOfLosses is present, otherwise the orderOfLosses overrules this
     * consideration.
     */
    @Test
    void testSelectCasualties_noLossOrder() {
      final DummyPlayer player = new DummyPlayer(null, false, "", null, true, 0, 0, false);
      final CasualtyDetails details =
          player.selectCasualties(
              unitPool,
              null,
              0,
              null,
              null,
              null,
              null,
              null,
              false,
              null,
              defaultCasualties,
              null,
              null,
              false);
      assertThat(details.getDamaged(), is(damaged));
      assertThat(details.getKilled(), is(List.of(unitPool.get(1), unitPool.get(2))));
      assertThat(details.getAutoCalculated(), is(false));
    }

    /**
     * This test checks if a present non-empty orderOfLosses argument correctly overrules any
     * "keepAtLeastOneLand" considerations. The result should be the same as in {@link
     * #testSelectCasualties_noLand}.
     */
    @Test
    void testSelectCasualties() {
      final DummyPlayer player = new DummyPlayer(null, false, "", orderOfLosses, true, 0, 0, false);
      final CasualtyDetails details =
          player.selectCasualties(
              unitPool,
              null,
              0,
              null,
              null,
              null,
              null,
              null,
              false,
              null,
              defaultCasualties,
              null,
              null,
              false);
      assertCommonCasualtyDetails(details);
      assertThat(details.getKilled(), is(List.of(unitPool.get(2), unitPool.get(0))));
    }
  }

  /**
   * This test checks if the arguments will be returned unaltered, when orderOfLosses is not present
   * and keepAtLeastOneLand is false.
   */
  @Test
  void testSelectCasualties_noLand_noLossOrder() {
    final DummyPlayer player = new DummyPlayer(null, false, "", null, false, 0, 0, false);
    final CasualtyDetails details =
        player.selectCasualties(
            unitPool,
            null,
            0,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            defaultCasualties,
            null,
            null,
            false);
    assertCommonCasualtyDetails(details);
    assertThat(details.getKilled(), is(killed));
  }

  /**
   * This test checks if units are correctly selected based on the loss order, ignoring that some
   * units might not be able to capture the target territory on their own. (Like air units for
   * example.)
   */
  @Test
  void testSelectCasualties_noLand() {
    final DummyPlayer player = new DummyPlayer(null, false, "", orderOfLosses, false, 0, 0, false);
    final CasualtyDetails details =
        player.selectCasualties(
            unitPool,
            null,
            0,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            defaultCasualties,
            null,
            null,
            false);
    assertCommonCasualtyDetails(details);
    assertThat(details.getKilled(), is(List.of(unitPool.get(2), unitPool.get(0))));
  }

  /**
   * This test checks if the case is handled correctly, where there are fewer entries in
   * orderOfLosses than there are Units in the selectFrom parameter of {@link
   * DummyPlayer#selectCasualties}.
   */
  @Test
  void testSelectCasualties_noLand_fewEntriesInLossOrder() {
    final DummyPlayer player =
        new DummyPlayer(
            null, false, "", Collections.singletonList(unitPool.get(0)), false, 0, 0, false);
    final CasualtyDetails details =
        player.selectCasualties(
            unitPool,
            null,
            0,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            defaultCasualties,
            null,
            null,
            false);
    assertCommonCasualtyDetails(details);
    assertThat(details.getKilled(), is(List.of(unitPool.get(0), unitPool.get(1))));
  }
}