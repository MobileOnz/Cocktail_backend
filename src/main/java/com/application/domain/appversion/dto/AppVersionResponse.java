package com.application.domain.appversion.dto;

/**
 * 앱이 실행할 때 받아 가는 버전 정보. 비교는 앱이 한다.
 *
 * @param latestVersion 스토어에 올라간 최신 버전. 현재 버전이 이보다 낮으면 닫을 수 있는 안내를 띄운다
 * @param minVersion    이보다 낮은 버전은 쓸 수 없다. 강제 업데이트 화면을 띄운다
 * @param storeUrl      업데이트 버튼이 여는 스토어 주소
 */
public record AppVersionResponse(String platform, String latestVersion, String minVersion, String storeUrl) {
}
