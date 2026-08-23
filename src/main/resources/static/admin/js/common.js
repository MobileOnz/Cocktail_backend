/* ===============================================
   🍸 Common JS (공통 함수)
   - Axios 기본 설정 + REST API helper 함수
   =============================================== */

// ✅ 컨텍스트 패스(/onz) 자동 감지 — 절대경로 "/admin/..." 요청이 context-path 를 빼먹어 404 나던
//    문제(QA P1-2) 해결. 어드민 페이지는 "/onz/admin/..." 로 서빙되므로 "/admin" 앞부분을 컨텍스트로 잡는다.
//    axios.defaults.baseURL 에 넣으면 이후 모든 axios/api 요청의 "/admin/..." 앞에 자동으로 붙는다.
//    window.location.href 같은 순수 이동은 axios 를 안 타므로 window.CONTEXT_PATH 로 직접 붙인다.
const CONTEXT_PATH = (() => {
    const p = window.location.pathname;
    const i = p.indexOf("/admin");
    return i > 0 ? p.substring(0, i) : "";
})();
window.CONTEXT_PATH = CONTEXT_PATH;

// ✅ Axios 기본 설정
axios.defaults.baseURL = CONTEXT_PATH;
axios.defaults.headers.common["X-Requested-With"] = "XMLHttpRequest";
axios.defaults.headers.post["Content-Type"] = "application/json; charset=utf-8";

// ✅ 공통 에러 처리
function handleError(error) {
    console.error("❌ API Error:", error);

    let message = "An unexpected error occurred.";

    if (error.response) {
        // 서버 응답이 있는 경우
        const { status, data } = error.response;
        message = data?.message || `Server responded with ${status}`;
    } else if (error.request) {
        // 요청은 전송됐지만 응답이 없는 경우
        message = "No response from server.";
    } else {
        // 요청 설정 중 오류
        message = error.message;
    }

    showToast(message, 3000);
    return Promise.reject(error);
}

/**
 * 알림창 + 확인 버튼 (URL 이동 포함)
 * @param {string} message - 표시할 메시지
 * @param {string} [redirectUrl] - 확인 클릭 시 이동할 URL (없으면 단순 닫기)
 */
function showToast(message, redirectUrl = null) {
    // 기존 토스트 제거 (중복 방지)
    const existing = document.querySelector(".custom-toast");
    if (existing) existing.remove();

    // 토스트 컨테이너 생성
    const toast = document.createElement("div");
    toast.className = "custom-toast";
    toast.style.cssText = `
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: white;
        color: #333;
        border: 1px solid #ccc;
        padding: 20px 30px;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
        font-size: 15px;
        text-align: center;
        z-index: 10000;
        min-width: 250px;
        max-width: 400px;
        animation: fadeIn 0.2s ease-out;
    `;

    // 메시지
    const msg = document.createElement("p");
    msg.textContent = message;
    msg.style.marginBottom = "15px";
    toast.appendChild(msg);

    // 확인 버튼
    const okBtn = document.createElement("button");
    okBtn.textContent = "확인";
    okBtn.style.cssText = `
        padding: 6px 14px;
        background-color: #007bff;
        color: white;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-size: 14px;
    `;
    okBtn.addEventListener("click", () => {
        toast.remove();
        if (redirectUrl) {
            window.location.href = redirectUrl;
        }
    });

    toast.appendChild(okBtn);
    document.body.appendChild(toast);
}

// ✅ REST API 요청 공통 함수
async function apiRequest(method, url, data = null, config = {}) {
    try {
        const response = await axios({
            method,
            url,
            data,
            ...config,
        });
        return response.data;
    } catch (error) {
        return handleError(error);
    }
}


// ✅ 간단한 헬퍼 함수들
const api = {
    get: (url, params, config) => apiRequest("get", url, null, { params, ...config }),
    post: (url, data, config) => apiRequest("post", url, data, config),
    put: (url, data, config) => apiRequest("put", url, data, config),
    patch: (url, data, config) => apiRequest("patch", url, data, config),
    del: (url, config) => apiRequest("delete", url, null, config),
};


// ✅ 예시: ag-Grid 초기화 함수 (공통)
function createGrid(elementId, columnDefs, rowData = []) {
    const gridOptions = {
        columnDefs,
        rowData,
        defaultColDef: {
            resizable: true,
            sortable: true,
            filter: true,
        },
    };
    const gridDiv = document.querySelector(`#${elementId}`);
    new agGrid.Grid(gridDiv, gridOptions);
    return gridOptions;
}
