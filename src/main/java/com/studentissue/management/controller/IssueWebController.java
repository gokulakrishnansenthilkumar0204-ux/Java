package com.studentissue.management.controller;

import com.studentissue.management.dto.IssueCreateRequest;
import com.studentissue.management.dto.IssueResponse;
import com.studentissue.management.dto.IssueStatsDto;
import com.studentissue.management.service.IssueRules;
import com.studentissue.management.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class IssueWebController {

    private final IssueService issueService;

    public IssueWebController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "votes") String sortBy,
            @RequestParam(required = false) String browserId,
            Model model) {

        String term = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : (search != null ? search.trim() : "");
        List<IssueResponse> issues = issueService.getAllIssues(term, category, status, sortBy, browserId);
        IssueStatsDto stats = issueService.getStatistics();
        List<String> categories = issueService.getCategories();

        model.addAttribute("issues", issues);
        model.addAttribute("stats", stats);
        model.addAttribute("categories", categories);
        model.addAttribute("search", term);
        model.addAttribute("currentCategory", category != null ? category : "ALL");
        model.addAttribute("currentStatus", status != null ? status : "ALL");
        model.addAttribute("currentSort", sortBy != null ? sortBy : "votes");
        model.addAttribute("confirmationsRequired", IssueRules.CONFIRMATIONS_REQUIRED_FOR_RESOLUTION);
        model.addAttribute("newIssue", new IssueCreateRequest());

        return "index";
    }

    /**
     * Fallback for browsers with JavaScript disabled.
     * (With JavaScript on, app.js submits the form to POST /api/issues instead.)
     */
    @PostMapping("/issues/report")
    public String reportIssueForm(
            @Valid @ModelAttribute("newIssue") IssueCreateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .distinct()
                    .collect(Collectors.joining(" "));
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to report issue: " + errors);
            return "redirect:/";
        }

        try {
            issueService.createIssue(request, null);
            redirectAttributes.addFlashAttribute("successMessage", "Issue reported successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to report issue: " + e.getMessage());
        }
        return "redirect:/";
    }
}
