package nextstep.subway.path.domain;

import static nextstep.subway.path.domain.PathFinder.*;
import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import nextstep.subway.exception.BadRequestException;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.Section;
import nextstep.subway.path.dto.PathResponse;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.dto.StationResponse;

class PathFinderTest {
    private List<Line> lines;
    private Station 강남역;
    private Station 양재역;
    private Station 교대역;
    private Station 남부터미널역;

    /**
     * 교대역    --- *2호선* ---   강남역
     * |                        |
     * *3호선*                   *신분당선*
     * |                        |
     * 남부터미널역 --- *3호선* --- 양재역
     */
    @BeforeEach
    void setUp() {
        // given
        강남역 = new Station(1L, "강남역");
        양재역 = new Station(2L, "양재역");
        교대역 = new Station(3L, "교대역");
        남부터미널역 = new Station(4L, "남부터미널역");

        final Line 신분당선 = new Line("신분당선", "red", 강남역, 양재역, 10, 300);
        final Line 이호선 = new Line("2호선", "green", 교대역, 강남역, 10, 0);
        final Line 삼호선 = new Line("3호선", "orange", 교대역, 양재역, 5, 500);
        삼호선.addSection(new Section(삼호선, 교대역, 남부터미널역, 3));

        lines = Arrays.asList(신분당선, 이호선, 삼호선);
    }

    @ParameterizedTest
    @CsvSource(value = {"5:0", "12:750", "18:1200", "19:1850"}, delimiter = ':')
    void computePath(int memberAge, int expectedFare) {
        // when
        final PathResponse pathResponse = PathFinder.computePath(lines, 남부터미널역, 강남역, memberAge);

        // then
        assertThat(pathResponse.getStations())
            .containsExactly(StationResponse.of(남부터미널역), StationResponse.of(양재역), StationResponse.of(강남역));
        assertThat(pathResponse.getDistance()).isEqualTo(12);
        assertThat(pathResponse.getFare()).isEqualTo(expectedFare);
    }

    @DisplayName("출발역과 도착역이 같을 경우 예외 발생")
    @Test
    void computePathOnSameSourceAndTarget() {
        // when, then
        assertThatThrownBy(() -> PathFinder.computePath(lines, 남부터미널역, 남부터미널역, 20))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining(SAME_SOURCE_TARGET_ERR_MSG);
    }

    @DisplayName("출발역과 도착역이 연결되어있지 않은 경우 예외 발생")
    @Test
    void computeNotConnectedPath() {
        // given
        final Station 송내역 = new Station(5L, "송내역");
        final Station 의정부역 = new Station(6L, "의정부역");
        final Line 일호선 = new Line("1호선", "indigo", 송내역, 의정부역, 10);
        final ArrayList<Line> newLines = new ArrayList<>(lines);
        newLines.add(일호선);

        // when, then
        assertThatThrownBy(() -> PathFinder.computePath(newLines, 송내역, 강남역, 20))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining(NOT_CONNECTED_STATIONS_ERR_MSG);
    }

    @DisplayName("노선이 하나도 없을 때 경로를 검색할 경우 예외 발생")
    @Test
    void computePathWithNoLines() {
        // given
        final Station 송내역 = new Station(5L, "송내역");
        final Station 의정부역 = new Station(6L, "의정부역");
        final ArrayList<Line> newLines = new ArrayList<>();

        // when, then
        assertThatThrownBy(() -> PathFinder.computePath(newLines, 송내역, 강남역, 20))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining(EMPTY_LINES_ERR_MSG);
    }
}
