package nextstep.subway.path.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.WeightedMultigraph;

import nextstep.subway.exception.BadRequestException;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.Section;
import nextstep.subway.path.dto.PathResponse;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.dto.StationResponse;

public class PathFinder {
    static final String EMPTY_LINES_ERR_MSG = "노선이 없을 때 경로를 조회할 수 없습니다.";
    static final String SAME_SOURCE_TARGET_ERR_MSG = "출발역과 도착역은 같을 수 없습니다.";
    static final String NOT_CONNECTED_STATIONS_ERR_MSG = "출발역과 도착역이 연결되어 있지 않습니다.";
    private static WeightedMultigraph<Station, WeightedSection> graph;

    private PathFinder() {
    }

    public static PathResponse computePath(
        final Collection<Line> lines, final Station source, final Station target, final int memberAge
    ) {
        validate(lines, source, target);
        initializeGraph(lines);
        final GraphPath<Station, WeightedSection> graphPath = getGraphPath(source, target);

        final List<StationResponse> stations = getStationsOnPath(graphPath);
        final Set<Line> pathLines = getLinesOnPath(graphPath);
        final int distance = (int)graphPath.getWeight();

        final int fare = FareCalculator.calculateFare(pathLines, distance);
        final int discountedFare = AgeGroup.of(memberAge).applyDiscount(fare);

        return new PathResponse(stations, distance, discountedFare);
    }

    private static void validate(final Collection<Line> lines, final Station source, final Station target) {
        if (lines.isEmpty()) {
            throw new BadRequestException(EMPTY_LINES_ERR_MSG);
        }
        if (source.equals(target)) {
            throw new BadRequestException(SAME_SOURCE_TARGET_ERR_MSG);
        }
    }

    private static void initializeGraph(final Collection<Line> lines) {
        graph = new WeightedMultigraph<>(WeightedSection.class);
        for (final Line line : lines) {
            setStationsAsVertex(line);
            setSectionsAsEdge(line);
        }
    }

    private static GraphPath<Station, WeightedSection> getGraphPath(final Station source, final Station target) {
        final GraphPath<Station, WeightedSection> graphPath =
            new DijkstraShortestPath<>(graph).getPath(source, target);
        if (graphPath == null) {
            throw new BadRequestException(NOT_CONNECTED_STATIONS_ERR_MSG);
        }
        return graphPath;
    }

    private static List<StationResponse> getStationsOnPath(final GraphPath<Station, WeightedSection> graphPath) {
        return graphPath.getVertexList().stream()
            .map(StationResponse::of)
            .collect(Collectors.toList());
    }

    private static Set<Line> getLinesOnPath(final GraphPath<Station, WeightedSection> graphPath) {
        return graphPath.getEdgeList().stream()
            .map(WeightedSection::getLine)
            .collect(Collectors.toSet());
    }

    private static void setStationsAsVertex(final Line line) {
        for (final Station station : line.extractAllStations()) {
            graph.addVertex(station);
        }
    }

    private static void setSectionsAsEdge(final Line line) {
        for (final Section section : line.getSections()) {
            final WeightedSection weightedSection = new WeightedSection(section);
            graph.addEdge(section.getUpStation(), section.getDownStation(), weightedSection);
            graph.setEdgeWeight(weightedSection, section.getDistance());
        }
    }
}
