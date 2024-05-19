package project.coca.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.oxm.ValidationFailureException;
import org.springframework.web.bind.annotation.*;
import project.coca.domain.group.CoGroup;
import project.coca.domain.group.GroupNotice;
import project.coca.domain.personal.Member;
import project.coca.domain.tag.GroupTag;
import project.coca.dto.request.GroupRequest;
import project.coca.dto.request.GroupUpdateRequest;
import project.coca.dto.response.common.ApiResponse;
import project.coca.dto.response.common.error.AlreadyReportedException;
import project.coca.dto.response.common.error.ErrorCode;
import project.coca.dto.response.common.success.ResponseCode;
import project.coca.dto.response.group.*;
import project.coca.service.GroupService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/group")
public class GroupController {
    private final GroupService groupService;

    /**
     * a1. 그룹에 참가하기
     *
     * @param request : Member(id면 충분), CoGroup(id, 경우에 따라 pwd)
     */
    @PostMapping("/join")
    public ApiResponse<?> joinGroup(@RequestBody GroupRequest request) {
        Member member = request.getMember();
        CoGroup group = request.getGroup();
        log.info("joinGroup : member {}", member);
        log.info("joinGroup : group {}", group);
        try {
            groupService.joinGroup(member, group);
            return ApiResponse.success(ResponseCode.OK, "그룹 참가 완료");
        } catch (NoSuchElementException e) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, e.getMessage());
        } catch (ValidationFailureException | AlreadyReportedException e) {
            return ApiResponse.fail(ErrorCode.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * a2. 그룹 탈퇴하기
     *
     * @param memberId : 회원 id
     * @param groupId  : 그룹 id
     */
    @PostMapping("/leave/member/{memberId}/group/{groupId}")
    public ApiResponse<?> leaveGroup(@PathVariable String memberId,
                                     @PathVariable Long groupId) {
        log.info("leaveGroup : member {}", memberId);
        log.info("leaveGroup : group {}", groupId);
        try {
            groupService.leaveGroup(memberId, groupId);
            return ApiResponse.success(ResponseCode.OK, "그룹 나가기 완료");
        } catch (NoSuchElementException e) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, e.getMessage());
        } catch (ValidationFailureException e) {
            return ApiResponse.fail(ErrorCode.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 21. 그룹 등록
     *
     * @param request : Member(id면 충분), CoGroup(*name, *description, password, notice)
     * @return 생성한 Group에 대한 정보
     */
    @PostMapping("/add")
    public ApiResponse<GroupDetailResponse> addGroup(@RequestBody GroupRequest request) {
        Member creator = request.getMember();
        CoGroup group = request.getGroup();
        List<GroupTag> groupTags = request.getGroupTags();
        log.info("Add group creator: {}", creator);
        log.info("Add group group: {}", group);
        log.info("Add group group tags: {}", groupTags);
        if (groupTags.size() > 3) {
            return ApiResponse.fail(ErrorCode.BAD_REQUEST, "태그 수는 3개 이하이어야 합니다.");
        }
        try {
            CoGroup savedGroup = groupService.saveGroup(creator, group, groupTags);
            GroupDetailResponse data = GroupDetailResponse.of(savedGroup);
            return ApiResponse.success(ResponseCode.CREATED, "그룹 정상 등록", data);
        } catch (NoSuchElementException e) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 22-1. 그룹 검색 by groupName
     *
     * @param groupName : 검색어
     * @param pageNum   : n 페이지
     * @return 그룹 id, 그룹명, 프라이빗여부, 멤버 여부, 태그 목록(id,필드,태그명), 회원 수
     */
    @GetMapping("/find/groupName/{groupName}/pageNum/{pageNum}")
    public ApiResponse<List<GroupResponse>> findGroupsByGroupName(@PathVariable String groupName,
                                                                  @PathVariable Integer pageNum) {
        log.info("Search group by title: {}", groupName);
        List<CoGroup> groups = groupService.findGroupsByNameLike(groupName, pageNum - 1);
        List<GroupResponse> data = groups
                .stream()
                .map(GroupResponse::of)
                .collect(Collectors.toList());
        return ApiResponse.response(ResponseCode.OK, data);
    }

    /**
     * 22-2. 그룹 검색 by tag
     *
     * @param tagName : tag 검색어
     * @param pageNum : n 페이지
     * @return 그룹 id, 그룹명, 프라이빗여부, 멤버 여부, 태그 목록(id,필드,태그명), 회원 수
     */
    @GetMapping("/find/tag/{tagName}/pageNum/{pageNum}")
    public ApiResponse<List<GroupResponse>> findGroupsByTag(@PathVariable String tagName,
                                                            @PathVariable Integer pageNum) {
        log.info("Search group by tag: {}", tagName);
        List<CoGroup> groups = groupService.findGroupsByTag(tagName, pageNum - 1);
        List<GroupResponse> data = groups
                .stream()
                .map(GroupResponse::of)
                .collect(Collectors.toList());
        return ApiResponse.response(ResponseCode.OK, data);
    }

    /**
     * 23. 그룹 상세 정보 조회
     *
     * @param memberId : 회원 id
     * @param groupId  : 그룹 id
     * @return 그룹id, 그룹명, admin정보(id,이름,프사경로), 그룹 설명, 프라이빗 여부, 그룹 태그 list(id, field, name), 회원 수
     */
    @GetMapping("/detail")
    public ApiResponse<GroupDetailSearchResponse> findGroupDetail(@RequestParam String memberId, @RequestParam Long groupId) {
        log.info("find group detail for search memberId: {}", memberId);
        log.info("find group detail for search by groupId: {}", groupId);
        try {
            GroupDetailSearchResponse data = groupService.findGroupById(memberId, groupId);
            return ApiResponse.response(ResponseCode.OK, data);
        } catch (NoSuchElementException e) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 25. 그룹 수정
     * 27. 그룹 공지 등록
     * 29. 그룹 공지 수정
     * 30. 그룹 공지 삭제
     *
     * @param request : Member(id면 충분), CoGroup(*id, *name, *description, password, notice)
     */
    @PutMapping("/update")
    public ApiResponse<?> updateGroup(@RequestBody GroupUpdateRequest request) {
        Member member = request.getAdmin();
        CoGroup group = request.getGroup();
        List<GroupTag> groupTags = request.getGroupTags();
        GroupNotice notice = request.getNotice();
        log.info("update group admin : {}", member);
        log.info("update group: {}", group);
        log.info("update group tags: {}", groupTags);
        log.info("update group notice: {}", notice);
        if (groupTags.size() > 3) {
            return ApiResponse.fail(ErrorCode.BAD_REQUEST, "태그 수는 3개 이하이어야 합니다.");
        }
        try {
            groupService.updateGroup(group.getId(), member.getId(), group, groupTags, notice);
            return ApiResponse.success(ResponseCode.OK, "수정 완료");
        } catch (NoSuchElementException e) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, e.getMessage());
        } catch (ValidationFailureException e) {
            return ApiResponse.fail(ErrorCode.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 25-a. Admin 을 위한 그룹 상세 정보 조회
     *
     * @param request : Member(id, pwd), CoGroup(id)
     * @return :Group(id,name,description,pwd
     * ,tagList(id,field,name)
     * ,members(id,name,프사경로)
     * ,managers(id,name,프사경로)
     * ,notice
     */
    @PostMapping("/admin")
    public ApiResponse<GroupAdminResponse> findGroupForAdmin(@RequestBody GroupRequest request) {
        Member member = request.getMember();
        CoGroup group = request.getGroup();
        log.info("find group for admin member : {}", member);
        log.info("find group for admin group: {}", group);
        try {
            CoGroup findGroup = groupService.findGroupForAdmin(group, member);
            GroupAdminResponse data = GroupAdminResponse.of(findGroup);
            return ApiResponse.response(ResponseCode.OK, data);
        } catch (NoSuchElementException e) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, e.getMessage());
        } catch (ValidationFailureException e) {
            return ApiResponse.fail(ErrorCode.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 26. 그룹 삭제 -> 보안적으로 취약한 편
     *
     * @param adminId : admin 계정
     * @param groupId : 그룹 id
     */
    @DeleteMapping("/delete")
    public ApiResponse<GroupResponse> deleteGroup(@RequestParam String adminId, @RequestParam Long groupId) {
        log.info("delete group admin: {}", adminId);
        log.info("delete group: {}", groupId);
        try {
            groupService.deleteGroup(adminId, groupId);
            return ApiResponse.success(ResponseCode.OK, "삭제 완료");
        } catch (NoSuchElementException e) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, e.getMessage());
        } catch (ValidationFailureException e) {
            return ApiResponse.fail(ErrorCode.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 28. 그룹 공지 조회
     *
     * @param memberId :회원 id
     * @param groupId  : 그룹 id
     * @return
     */
    @GetMapping("/notice")
    public ApiResponse<GroupNoticeResponse> findGroupNotice(@RequestParam String memberId, @RequestParam Long groupId) {
        log.info("find group notice memberId: {}", memberId);
        log.info("find group notice groupId: {}", groupId);
        try {
            GroupNotice notice = groupService.findGroupNotice(memberId, groupId);
            GroupNoticeResponse data = GroupNoticeResponse.of(notice);
            return ApiResponse.response(ResponseCode.OK, data);
        } catch (NoSuchElementException e) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, e.getMessage());
        } catch (ValidationFailureException e) {
            return ApiResponse.fail(ErrorCode.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
