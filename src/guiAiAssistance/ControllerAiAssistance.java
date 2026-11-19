package guiAiAssistance;

import aiAssistance.*;
import lessonManagement.*;
import javafx.concurrent.Task;
import java.util.ArrayList;
import java.util.List;

/*******
 * <p> Title: ControllerAiAssistance Class. </p>
 * <p> Description: Runs external work off the JavaFX thread; cancellation and task identity
 * prevent late responses from overwriting current UI. Suggestions are never automatically
 * accepted or saved. All actions are validated by TP3AiGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 AI MVC
 */
public class ControllerAiAssistance {
    private static final List<AiTurn> history = new ArrayList<>();
    private static AiSuggestion candidate;
    private static Task<OperationResult<AiSuggestion>> running;
    private ControllerAiAssistance() { }
    /** @return unsaved draft values; tested by AI GUI draft retention */
    protected static LessonDraft draft() { return new LessonDraft(ViewAiAssistance.title.getText(), ViewAiAssistance.problem.getText(), ViewAiAssistance.text.getText()); }
    /** Prepare on FX thread, generate off-thread, reauthorize before displaying; tested by AI GUI follow-ups. */
    protected static void ask() {
        if (running != null) return;
        AiRequest request = new AiRequest(ViewAiAssistance.purpose.getValue(), ViewAiAssistance.lessonId, draft(), ViewAiAssistance.prompt.getText(), history);
        var prepared = ModelAiAssistance.prepare(request);
        if (!prepared.isSuccessful()) { ViewAiAssistance.status.setText(prepared.getMessage()); return; }
        discard(); busy(true); ViewAiAssistance.status.setText("AI request in progress. Your draft has not been saved.");
        Task<OperationResult<AiSuggestion>> task = new Task<>() {
            @Override protected OperationResult<AiSuggestion> call() { return ModelAiAssistance.generate(prepared.getValue()); }
        };
        running = task;
        task.setOnSucceeded(e -> {
            if (running != task) return;
            running = null; busy(false);
            var access = ModelAiAssistance.prepare(request);
            var result = task.getValue();
            if (!access.isSuccessful()) { ViewAiAssistance.status.setText(access.getMessage()); return; }
            if (result == null || !result.isSuccessful()) { ViewAiAssistance.status.setText(result == null ? "AI returned no usable result. Retry manually." : result.getMessage()); return; }
            candidate = result.getValue(); history.add(new AiTurn("user", request.getPrompt())); history.add(new AiTurn("assistant", candidate.getConversationText()));
            StringBuilder output = new StringBuilder("AI-GENERATED SUGGESTION — review before use\n\n").append(candidate.getText());
            if (candidate.getCore() != null) output.append("\n\nProposed title: ").append(candidate.getCore().getTitle()).append("\nProposed problem: ").append(candidate.getCore().getProblemSituation()).append("\nProposed lesson: ").append(candidate.getCore().getLessonLearned());
            if (!candidate.getTerms().isEmpty()) output.append("\n\nSuggested terms:\n").append(String.join("\n", candidate.getTerms()));
            output.append(candidate.getSources().isEmpty() ? "\n\nNo retrieved source links supplied." : "\n\nRetrieved resource links (provider citations; copy to browser):");
            for (var source : candidate.getSources()) output.append("\n").append(source.getTitle()).append("\n").append(source.getUrl());
            ViewAiAssistance.suggestion.setText(output.toString()); ViewAiAssistance.accept.setDisable(candidate.getCore() == null && candidate.getTerms().isEmpty());
            ViewAiAssistance.status.setText(result.getMessage());
        });
        task.setOnFailed(e -> { if (running == task) { running = null; busy(false); ViewAiAssistance.status.setText("AI request failed. Your draft is unchanged; retry or continue manually."); } });
        Thread worker = new Thread(task, "lesson-ai-request"); worker.setDaemon(true); worker.start();
    }
    /** Cancel and invalidate callbacks; tested by AI GUI cancellation and late-response checks. */
    protected static void cancel() {
        Task<?> old = running; running = null; if (old != null) old.cancel(true);
        if (ViewAiAssistance.ask != null) { busy(false); ViewAiAssistance.status.setText("AI request canceled. Your draft is unchanged."); }
    }
    /** Apply only explicitly selected suggestions to editable draft controls; tested by AI GUI acceptance. */
    protected static void accept() {
        if (candidate == null) return;
        if (ViewAiAssistance.purpose.getValue() == AiPurpose.TERMS) ViewAiAssistance.terms.setText(String.join("\n", candidate.getTerms()));
        else if (candidate.getCore() != null) {
            ViewAiAssistance.title.setText(candidate.getCore().getTitle()); ViewAiAssistance.problem.setText(candidate.getCore().getProblemSituation()); ViewAiAssistance.text.setText(candidate.getCore().getLessonLearned());
        }
        ViewAiAssistance.status.setText("Suggestion copied into the draft. Review and edit it before choosing Save.");
    }
    /** Drop generated output without touching draft or saved data; tested by AI GUI discard. */
    protected static void discard() { candidate = null; if (ViewAiAssistance.suggestion != null) ViewAiAssistance.suggestion.clear(); if (ViewAiAssistance.accept != null) ViewAiAssistance.accept.setDisable(true); }
    /** Start fresh bounded history without discarding draft; tested by AI GUI conversation reset. */
    protected static void newConversation() { cancel(); history.clear(); discard(); ViewAiAssistance.status.setText("Enter a request. AI output remains unsaved until you review and save it."); }
    /** Explicit reviewed save follows normal permission and lock rules; tested by AI GUI saves and locks. */
    protected static void saveCore() {
        var saved = ModelAiAssistance.saveCore(); if (saved.isSuccessful()) ViewAiAssistance.lessonId = saved.getValue().getId(); ViewAiAssistance.status.setText(saved.getMessage());
    }
    /** Explicit reviewed Curator term save; tested by AI GUI terms and rejected blank values. */
    protected static void saveTerms() { var saved = ModelAiAssistance.saveTerms(); ViewAiAssistance.status.setText(saved.getMessage()); }
    /** Cancel before navigation so late work cannot update another page; tested by AI GUI return. */
    protected static void back() {
        cancel(); if (guiTools.RoleNames.CURATOR.equals(ViewAiAssistance.role)) guiCuratorLessons.ViewCuratorLessons.displayCuratorLessons(ViewAiAssistance.stage, ViewAiAssistance.user);
        else guiContributorLessons.ViewContributorLessons.displayLessons(ViewAiAssistance.stage, ViewAiAssistance.user);
    }
    private static void busy(boolean busy) {
        ViewAiAssistance.ask.setDisable(busy); ViewAiAssistance.cancel.setDisable(!busy); ViewAiAssistance.purpose.setDisable(busy);
        ViewAiAssistance.saveCore.setDisable(busy); ViewAiAssistance.saveTerms.setDisable(busy); ViewAiAssistance.fresh.setDisable(busy);
    }
}
