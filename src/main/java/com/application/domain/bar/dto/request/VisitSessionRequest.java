package com.application.domain.bar.dto.request;

/**
 * @param qrPayload            스캔한 QR. 없으면 GPS 만으로 L1(채팅만) 판정. 구버전 앱 하위호환.
 * @param mockLocationDetected 클라이언트가 보고한 모의위치 여부.
 *                             신뢰할 수 없는 값이지만 정직한 앱을 강등시키는 데는 충분하다.
 */
public record VisitSessionRequest(
        String qrPayload,
        Double lat,
        Double lng,
        Double accuracyM,
        Boolean mockLocationDetected
) {}
