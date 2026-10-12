package com.kamsarobar.admin;

import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kamsarobar.access.AccessPolicy;
import com.kamsarobar.admin.dto.MemberDetails;
import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.common.exception.ForbiddenException;
import com.kamsarobar.common.util.TextNormalizer;
import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.donation.DonationRepository;
import com.kamsarobar.post.CommentRepository;
import com.kamsarobar.post.EventAttendeeRepository;
import com.kamsarobar.post.PostRepository;
import com.kamsarobar.profile.ProfileService;
import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.User;
import com.kamsarobar.user.UserRepository;
import com.kamsarobar.user.UserService;
import com.kamsarobar.user.dto.UserResponse;

/**
 * Member management shared by the main admin (every city) and city admins (only the city they manage):
 * listing members, viewing a member's full profile, and blocking / unblocking.
 */
@Service
@Transactional(readOnly = true)
public class MemberAdminService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final ProfileService profileService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final DonationRepository donationRepository;
    private final EventAttendeeRepository attendeeRepository;
    private final AccessPolicy accessPolicy;

    public MemberAdminService(UserRepository userRepository, UserService userService, ProfileService profileService,
                              PostRepository postRepository, CommentRepository commentRepository,
                              DonationRepository donationRepository, EventAttendeeRepository attendeeRepository,
                              AccessPolicy accessPolicy) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.profileService = profileService;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.donationRepository = donationRepository;
        this.attendeeRepository = attendeeRepository;
        this.accessPolicy = accessPolicy;
    }

    /** City admins only ever get their own city's members, whatever cityId they ask for. */
    public PageResponse<UserResponse> list(UserPrincipal actor, String query, Long cityId, MemberStatus status,
                                           Pageable pageable) {
        Long scope = cityId;
        if (!actor.isMainAdmin()) {
            if (cityId != null && !cityId.equals(actor.managedCityId())) {
                throw new ForbiddenException("You can only see members of the city you manage");
            }
            scope = actor.managedCityId();
        }
        Boolean blocked = status == null ? null : status == MemberStatus.BLOCKED;
        String key = TextNormalizer.key(query);
        String pattern = key == null ? null : TextNormalizer.likeContains(key);
        return PageResponse.of(userRepository.search(scope, blocked, pattern, pageable), UserResponse::from);
    }

    public MemberDetails details(Long memberId, UserPrincipal actor) {
        User member = userService.getEntity(memberId);
        accessPolicy.requireCityManager(actor, member.getCity().getId());
        return toDetails(member, actor);
    }

    @Transactional
    public MemberDetails block(Long memberId, String reason, UserPrincipal actor) {
        if (Objects.equals(actor.id(), memberId)) {
            throw new BadRequestException("You cannot block yourself");
        }
        User member = requireBlockable(memberId, actor);
        member.block(TextNormalizer.clean(reason), userService.getEntity(actor.id()));
        return toDetails(member, actor);
    }

    @Transactional
    public MemberDetails unblock(Long memberId, UserPrincipal actor) {
        User member = requireBlockable(memberId, actor);
        member.unblock();
        return toDetails(member, actor);
    }

    private User requireBlockable(Long memberId, UserPrincipal actor) {
        User member = userService.getEntity(memberId);
        if (!accessPolicy.canBlockMember(actor, member.getId(), member.getRole(), member.getCity().getId())) {
            throw new ForbiddenException("You are not allowed to block or unblock this member");
        }
        return member;
    }

    private MemberDetails toDetails(User member, UserPrincipal actor) {
        Long id = member.getId();
        var activity = new MemberDetails.Activity(postRepository.countByAuthorId(id),
                commentRepository.countByAuthorId(id), donationRepository.countByDonorId(id),
                donationRepository.totalVerifiedByDonor(id), attendeeRepository.countByUserId(id));
        var block = new MemberDetails.BlockInfo(member.isBlocked(), member.getBlockedReason(), member.getBlockedAt(),
                member.getBlockedBy() == null ? null : member.getBlockedBy().getName());
        boolean canBlock = accessPolicy.canBlockMember(actor, id, member.getRole(), member.getCity().getId());
        // Only the main admin moves members between cities (city admins cannot even move themselves).
        return new MemberDetails(UserResponse.from(member), profileService.getForUser(id), activity, block, canBlock,
                actor.isMainAdmin());
    }
}
