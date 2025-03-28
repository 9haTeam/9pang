//package com._hateam.config;
//
//import com._hateam.dto.HubResponseDto;
//import com._hateam.entity.Hub;
//import com._hateam.entity.HubRoute;
//import com._hateam.repository.HubRepository;
//import com._hateam.repository.HubRouteRepository;
//import com._hateam.service.GraphPathFinder;
//import com._hateam.service.HubGraph;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cache.annotation.CachePut;
//import org.springframework.core.annotation.Order;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.text.Collator;
//import java.util.*;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class HubInitializer {
//
//    private final HubRepository hubRepository;
//    private final HubRouteRepository hubRouteRepository;
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    @PostConstruct
//    @Order(1)
//    @Transactional
//    public void initializeHubs() {
//        // 정적 허브 데이터 목록
//        List<HubResponseDto> hubDataList = Arrays.asList(
//                new HubResponseDto("서울", "서울특별시 송파구 송파대로 55", "37.4562557", "126.7052062"),
//                new HubResponseDto("경기북부", "경기도 고양시 덕양구 권율대로 570", "37.632348", "126.852348"),
//                new HubResponseDto("경기남부", "경기도 이천시 덕평로 257-21", "37.4562557", "126.7052062"),
//                new HubResponseDto("부산", "부산 동구 중앙대로 206", "35.1141634", "129.0345914"),
//                new HubResponseDto("대구", "대구 북구 태평로 161", "35.8828351", "128.5996849"),
//                new HubResponseDto("인천", "인천 남동구 정각로 29", "37.4475431", "126.7052062"),
//                new HubResponseDto("광주", "광주 서구 내방로 111", "35.1595454", "126.852348"),
//                new HubResponseDto("대전", "대전 서구 둔산로 100", "36.3504119", "127.3848135"),
//                new HubResponseDto("울산", "울산 남구 중앙로 201", "35.538564", "129.311359"),
//                new HubResponseDto("세종", "세종특별자치시 한누리대로 2130", "36.4801124", "127.2890587"),
//                new HubResponseDto("강원도", "강원특별자치도 춘천시 중앙로 1", "37.8812616", "127.7291884"),
//                new HubResponseDto("충청북도", "충북 청주시 상당구 상당로 82", "36.6353242", "127.4897855"),
//                new HubResponseDto("충청남도", "충남 홍성군 홍북읍 충남대로 21", "36.6025547", "126.6667362"),
//                new HubResponseDto("전라북도", "전북특별자치도 전주시 완산구 효자로 225", "35.8203627", "127.1068278"),
//                new HubResponseDto("전라남도", "전남 무안군 삼향읍 오룡길 1", "34.8492021", "126.4715102"),
//                new HubResponseDto("경상북도", "경북 안동시 풍천면 도청대로 455", "36.578652", "128.425883"),
//                new HubResponseDto("경상남도", "경남 창원시 의창구 중앙대로 300", "35.2343864", "128.6925514")
//        );
//
//        // 허브 저장 및 Redis 캐싱
//        Map<String, Hub> hubMap = new HashMap<>();
//        for (HubResponseDto data : hubDataList) {
//            Hub hub = saveHub(data.getName(), data.getAddress(), data.getLatitude(), data.getLongitude());
//            hubMap.put(data.getName(), hub);
//            log.info("Saved Hub: {}", hub.getName());
//        }
//
//        // HubGraph 객체 생성 및 경로 계산기 초기화
//        HubGraph hubGraph = new HubGraph();
//        Map<String, List<String>> graphStructure = hubGraph.getGraph();
//        GraphPathFinder pathFinder = new GraphPathFinder(hubGraph, hubMap);
//
//        // 직접 연결된 허브 경로(간선) DB 및 Redis 저장 (중복 방지를 위해 사전순 비교)
//        Collator collator = Collator.getInstance(Locale.KOREAN);
//        for (String sourceName : graphStructure.keySet()) {
//            for (String neighbor : graphStructure.get(sourceName)) {
//                if (collator.compare(sourceName, neighbor) < 0) {
//                    Hub source = hubMap.get(sourceName);
//                    Hub dest = hubMap.get(neighbor);
//                    if (source != null && dest != null) {
//                        try {
//                            double lat1 = Double.parseDouble(source.getLatitude());
//                            double lon1 = Double.parseDouble(source.getLongitude());
//                            double lat2 = Double.parseDouble(dest.getLatitude());
//                            double lon2 = Double.parseDouble(dest.getLongitude());
//                            double distance = pathFinder.haversine(lat1, lon1, lat2, lon2);
//
//                            // 직접 연결인 경우, 모든 정보를 동일한 값으로 초기화
//                            Double penaltyDistance = 0.0;
//                            List<Double> nodeDistances = Collections.singletonList(distance);
//                            List<String> route = Arrays.asList(source.getName(), dest.getName());
//                            Double actualDistance = distance;
//                            Double totalCost = distance;
//                            List<Double> cumulativeDistances = Collections.singletonList(distance);
//
//                            // 모든 정보를 저장하는 메서드 호출
//                            saveHubRoute(source, dest, distance, penaltyDistance, nodeDistances, route, actualDistance, totalCost, cumulativeDistances);
//                            log.info("Saved direct HubRoute: {} -> {} with distance {} km", sourceName, neighbor, distance);
//                        } catch (NumberFormatException e) {
//                            log.warn("Invalid coordinate format for hub route {} -> {}", sourceName, neighbor);
//                        }
//                    }
//                }
//            }
//        }
//
//
//        // 모든 허브 쌍에 대해 최단 경로 계산 후 DB와 Redis에 저장
//        List<String> hubNames = new ArrayList<>(hubMap.keySet());
//        for (String start : hubNames) {
//            for (String end : hubNames) {
//                if (!start.equals(end)) {
//                    GraphPathFinder.PathResult result = pathFinder.findMinimumCostPath(start, end);
//                    if (result != null) {
//                        saveHubRoute(
//                                hubMap.get(start),
//                                hubMap.get(end),
//                                result.getActualDistance(),    // distance
//                                result.getPenalty(),           // penaltyDistance
//                                result.getSegmentDistances(),  // nodeDistances
//                                result.getPath(),              // route (경로 리스트)
//                                result.getActualDistance(),    // actualDistance
//                                result.getTotalCost(),         // totalCost
//                                result.getNodeDistances()      // cumulativeDistances
//                        );
//                        log.info("Computed and saved route from {} to {}: Total Cost: {} km, Path: {}",
//                                start, end, result.getTotalCost(), result.getPath());
//                    } else {
//                        log.warn("No path found from {} to {}", start, end);
//                    }
//                }
//            }
//        }
//
//        // 직접 연결된 간선의 기본 Haversine 거리를 Redis에 별도로 캐시 (필요 시 사용)
//        Map<String, Double> directEdges = new HashMap<>();
//        for (String sourceName : graphStructure.keySet()) {
//            for (String neighbor : graphStructure.get(sourceName)) {
//                Hub source = hubMap.get(sourceName);
//                Hub dest = hubMap.get(neighbor);
//                if (source != null && dest != null) {
//                    try {
//                        double lat1 = Double.parseDouble(source.getLatitude());
//                        double lon1 = Double.parseDouble(source.getLongitude());
//                        double lat2 = Double.parseDouble(dest.getLatitude());
//                        double lon2 = Double.parseDouble(dest.getLongitude());
//                        double distance = pathFinder.haversine(lat1, lon1, lat2, lon2);
//                        directEdges.put(sourceName + ":" + neighbor, distance);
//                        log.info("Direct edge {} -> {}: {} km", sourceName, neighbor, distance);
//                    } catch (NumberFormatException e) {
//                        log.warn("Invalid coordinate format for direct edge {} -> {}", sourceName, neighbor);
//                    }
//                }
//            }
//        }
//        redisTemplate.opsForHash().putAll("hubGraphDirect", directEdges);
//        log.info("Direct hub graph saved to Redis.");
//    }
//
//    @CachePut(value = "hub", key = "#result.id")
//    public Hub saveHub(String name, String address, String latitude, String longitude) {
//        Hub hub = Hub.builder()
//                .name(name)
//                .address(address)
//                .latitude(latitude)
//                .longitude(longitude)
//                .build();
//        Hub savedHub = hubRepository.save(hub);
//        // Redis에 hub의 전체 정보를 캐시
//        String hubKey = "hub:" + savedHub.getId();
//        Map<String, Object> hubData = new HashMap<>();
//        hubData.put("id", savedHub.getId());
//        hubData.put("name", savedHub.getName());
//        hubData.put("address", savedHub.getAddress());
//        hubData.put("latitude", savedHub.getLatitude());
//        hubData.put("longitude", savedHub.getLongitude());
//        redisTemplate.opsForHash().putAll(hubKey, hubData);
//        return savedHub;
//    }
//
//    // 직접 연결된 허브 경로를 위한 오버로딩 메서드 (최소 정보)
//    public void saveHubRoute(Hub source, Hub dest, double distance) {
//        // 다른 필드는 null 또는 기본값으로 처리
//        saveHubRoute(source, dest, distance, null, null, null, null, null, null);
//    }
//
//    // 전체 허브 경로 정보를 DB와 Redis에 저장하는 메서드
//    public void saveHubRoute(Hub source, Hub dest, double distance,
//                             Double penaltyDistance, List<Double> nodeDistances,
//                             List<String> route, Double actualDistance,
//                             Double totalCost, List<Double> cumulativeDistances) {
//
//        HubRoute hubRoute = HubRoute.builder()
//                .sourceHub(source)
//                .destinationHub(dest)
//                .distanceKm(Math.round(distance))
//                .estimatedTimeMinutes(0)
//                .penaltyDistance(penaltyDistance)
//                .nodeDistances(nodeDistances)
//                .route(route)
//                .actualDistance(actualDistance)
//                .totalCost(totalCost)
//                .cumulativeDistances(cumulativeDistances)
//                .build();
//
//        HubRoute savedRoute = hubRouteRepository.save(hubRoute);
//
//        // Redis에 저장할 키 구성 (예: hubRoute:서울:부산)
//        String routeKey = "hubRoute:" + source.getName() + ":" + dest.getName();
//        redisTemplate.opsForHash().putAll(routeKey, convertHubRouteToMap(savedRoute));
//    }
//    private Map<String, Object> convertHubRouteToMap(HubRoute hubRoute) {
//        Map<String, Object> routeData = new HashMap<>();
//        routeData.put("id", hubRoute.getId().toString());
//
//        // sourceHub와 destinationHub를 Redis에 저장하기 위해 serializable한 형태로 변환
//        Map<String, Object> sourceHubData = new HashMap<>();
//        sourceHubData.put("id", hubRoute.getSourceHub().getId().toString());
//        sourceHubData.put("name", hubRoute.getSourceHub().getName());
//        sourceHubData.put("address", hubRoute.getSourceHub().getAddress());
//        sourceHubData.put("latitude", hubRoute.getSourceHub().getLatitude());
//        sourceHubData.put("longitude", hubRoute.getSourceHub().getLongitude());
//        routeData.put("sourceHub", sourceHubData);
//
//        Map<String, Object> destHubData = new HashMap<>();
//        destHubData.put("id", hubRoute.getDestinationHub().getId().toString());
//        destHubData.put("name", hubRoute.getDestinationHub().getName());
//        destHubData.put("address", hubRoute.getDestinationHub().getAddress());
//        destHubData.put("latitude", hubRoute.getDestinationHub().getLatitude());
//        destHubData.put("longitude", hubRoute.getDestinationHub().getLongitude());
//        routeData.put("destinationHub", destHubData);
//
//        routeData.put("distanceKm", hubRoute.getDistanceKm());
//        routeData.put("estimatedTimeMinutes", hubRoute.getEstimatedTimeMinutes());
//        routeData.put("penaltyDistance", hubRoute.getPenaltyDistance());
//        routeData.put("nodeDistances", hubRoute.getNodeDistances());
//        routeData.put("route", hubRoute.getRoute());
//        routeData.put("actualDistance", hubRoute.getActualDistance());
//        routeData.put("totalCost", hubRoute.getTotalCost());
//        routeData.put("cumulativeDistances", hubRoute.getCumulativeDistances());
//
//        return routeData;
//    }
//}
