// static/js/comment.js
// ※ CSRF 자동 첨부는 base.html의 전역 ajaxSend가 처리한다. 여기서 중복 세팅하지 않는다.
$(function () {

    const $list = $("#comment-list");
    const $count = $("#comment-count");

    function changeCount(delta) {
        $count.text(parseInt($count.text(), 10) + delta);
    }

    // ── ① 원댓글 작성 ──
    $("#btn-submit-comment").on("click", function () {
        const postId = $(this).data("post-id");
        const content = $("#new-comment-content").val().trim();
        if (!content) { alert("댓글 내용을 입력하세요."); return; }

        $.ajax({
            url: `/posts/${postId}/comments`, method: "POST",
            data: { content: content }
        })      // parentId 없음 → 원댓글
            .done(function (html) {
                $list.append(html);                     // 목록 맨 끝에 삽입
                $("#new-comment-content").val("");
                changeCount(1);
            })
            .fail(function (xhr) { alert(xhr.responseText || "댓글 등록에 실패했습니다."); });
    });

    // ── ② [답글] 토글 (이벤트 위임 : 동적 추가 요소에도 먹힘) ──
    $list.on("click", ".btn-reply", function () {
        $(`#reply-form-${$(this).data("comment-id")}`).toggle();
    });

    // ── ③ 대댓글 작성 ──
    $list.on("click", ".btn-submit-reply", function () {
        const postId = $(this).data("post-id");
        const parentId = $(this).data("parent-id");
        const $textarea = $(`#reply-form-${parentId} .reply-content`);
        const content = $textarea.val().trim();
        if (!content) { alert("답글 내용을 입력하세요."); return; }

        $.ajax({
            url: `/posts/${postId}/comments`, method: "POST",
            data: { content: content, parentId: parentId }
        })  // parentId 실림 → 대댓글
            .done(function (html) {
                // ⚠ smell: 서버가 commentItem 통째를 주므로 대댓글만 부모 밑에 꽂기가 지저분함.
                //   깔끔한 해법 = 서버가 대댓글일 땐 replyItem 조각만 반환 (6-4 리뷰 ④).
                //   지금은 학습용으로 부모의 .replies에 통째 append.
                $(`#replies-${parentId}`).append(html);
                $textarea.val("");
                $(`#reply-form-${parentId}`).hide();
                changeCount(1);
            })
            .fail(function (xhr) { alert(xhr.responseText || "답글 등록에 실패했습니다."); });
    });

    // ── ④ 삭제 : 200/204 분기 ──
    $list.on("click", ".btn-delete-comment", function () {
        if (!confirm("댓글을 삭제하시겠습니까?")) return;
        const id = $(this).data("comment-id");

        $.ajax({ url: `/comments/${id}`, method: "DELETE" })
            .done(function (html, textStatus, xhr) {
                const $target = $(`#comment-${id}`);
                if (xhr.status === 204) {
                    $target.remove();               // 대댓글 없음 → DOM 제거
                } else {
                    $target.replaceWith(html);      // 대댓글 있음 → 플레이스홀더로 교체
                }
                changeCount(-1);
            })
            .fail(function (xhr) { alert(xhr.responseText || "삭제 권한이 없거나 실패했습니다."); });
    });
});