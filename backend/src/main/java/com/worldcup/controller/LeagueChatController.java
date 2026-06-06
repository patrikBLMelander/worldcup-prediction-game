package com.worldcup.controller;

import com.worldcup.dto.ChatMessageDTO;
import com.worldcup.dto.LeagueUnreadDTO;
import com.worldcup.dto.SendChatMessageRequest;
import com.worldcup.entity.User;
import com.worldcup.security.CurrentUser;
import com.worldcup.service.LeagueChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * League chat endpoints. All require the current user to be a member of the league
 * (enforced in {@link LeagueChatService}). Paths live under /api/chat to avoid
 * clashing with the /api/leagues/{id} routes.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class LeagueChatController {

    private final CurrentUser currentUser;
    private final LeagueChatService chatService;

    @GetMapping("/leagues/{leagueId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getMessages(@PathVariable Long leagueId) {
        User user = currentUser.getCurrentUserOrThrow();
        return ResponseEntity.ok(chatService.getMessages(leagueId, user));
    }

    @PostMapping("/leagues/{leagueId}/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @PathVariable Long leagueId,
            @Valid @RequestBody SendChatMessageRequest request) {
        User user = currentUser.getCurrentUserOrThrow();
        ChatMessageDTO created = chatService.postMessage(leagueId, user, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/leagues/{leagueId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long leagueId) {
        User user = currentUser.getCurrentUserOrThrow();
        chatService.markRead(leagueId, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread")
    public ResponseEntity<List<LeagueUnreadDTO>> unread() {
        User user = currentUser.getCurrentUserOrThrow();
        return ResponseEntity.ok(chatService.unreadSummary(user));
    }
}
