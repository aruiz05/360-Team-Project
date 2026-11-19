package guiAiAssistance;

import aiAssistance.*;
import entityClasses.User;
import guiTools.RoleNames;
import lessonManagement.LessonDraft;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/*******
 * <p> Title: ViewAiAssistance Class. </p>
 * <p> Description: Interactive writing/brainstorming/examples and Curator refinement/terms.
 * Generated suggestions, editable drafts and explicit persistence actions remain separate.
 * Validated by TP3AiGuiTestingAutomation and documented native Eclipse checks.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 AI MVC
 */
public class ViewAiAssistance {
    protected static Stage stage;
    protected static User user;
    protected static String role;
    protected static Long lessonId;
    /** Draft and reviewed term controls never receive automatic provider writes. */
    protected static TextField title;
    protected static TextArea problem, text, terms, prompt, suggestion;
    protected static ComboBox<AiPurpose> purpose;
    protected static Button ask, cancel, accept, saveCore, saveTerms, fresh;
    protected static Label status;
    private ViewAiAssistance() { }
    /**
     * @param target application stage
     * @param account authenticated snapshot
     * @param activeRole Contributor or Curator
     * @param id existing lesson or null for a new Contributor draft
     * @param initial current unsaved draft; null reloads existing saved context
     */
    public static void displayAssistance(Stage target, User account, String activeRole, Long id, LessonDraft initial) {
        ControllerAiAssistance.cancel();
        var current = guiTools.GUISingleRoleDispatch.requireRoleForView(target, account, activeRole);
        if (current == null) return;
        stage = target; user = current; role = activeRole; lessonId = id;
        if (id != null) {
            var loaded = ModelAiAssistance.load();
            if (!loaded.isSuccessful()) { guiUserLogin.ViewUserLogin.displayUserLogin(target); return; }
            if (initial == null) initial = new LessonDraft(loaded.getValue().getTitle(), loaded.getValue().getProblemSituation(), loaded.getValue().getLessonLearned());
        }
        if (initial == null) initial = new LessonDraft("", "", "");
        title = new TextField(initial.getTitle()); title.setId("ai-title"); problem = area("ai-problem", true); problem.setText(initial.getProblemSituation());
        text = area("ai-text", true); text.setText(initial.getLessonLearned()); terms = area("ai-terms", true); terms.setPrefRowCount(3);
        prompt = area("ai-prompt", true); prompt.setPromptText("Enter a request or follow-up question"); prompt.setPrefRowCount(3);
        suggestion = area("ai-suggestion", false);
        purpose = new ComboBox<>(); purpose.setId("ai-purpose");
        purpose.getItems().setAll(RoleNames.CURATOR.equals(role) ? java.util.List.of(AiPurpose.REFINE, AiPurpose.TERMS) : java.util.List.of(AiPurpose.WRITE, AiPurpose.BRAINSTORM, AiPurpose.EXAMPLES));
        purpose.getSelectionModel().selectFirst(); purpose.setOnAction(e -> ControllerAiAssistance.newConversation());
        ask = new Button("Ask AI / Follow Up"); cancel = new Button("Cancel Request"); accept = new Button("Accept Suggestion Into Draft");
        saveCore = new Button("Save Reviewed Draft"); saveTerms = new Button("Save Reviewed Terms"); fresh = new Button("New Conversation");
        Button discard = new Button("Discard Suggestion"), back = new Button("Return to Lessons");
        ask.setOnAction(e -> ControllerAiAssistance.ask()); cancel.setOnAction(e -> ControllerAiAssistance.cancel()); accept.setOnAction(e -> ControllerAiAssistance.accept());
        discard.setOnAction(e -> ControllerAiAssistance.discard()); fresh.setOnAction(e -> ControllerAiAssistance.newConversation());
        saveCore.setOnAction(e -> ControllerAiAssistance.saveCore()); saveTerms.setOnAction(e -> ControllerAiAssistance.saveTerms()); back.setOnAction(e -> ControllerAiAssistance.back());
        VBox draft = new VBox(6, new Label("Reviewed draft — saved only when you choose Save"), new Label("Title"), title,
                new Label("Problem or situation"), problem, new Label("Lesson learned"), text, saveCore);
        if (RoleNames.CURATOR.equals(role)) draft.getChildren().addAll(new Label("Reviewed terms (one per line)"), terms, saveTerms);
        VBox.setVgrow(problem, Priority.ALWAYS); VBox.setVgrow(text, Priority.ALWAYS);
        VBox assistance = new VBox(8, new Label("AI-generated suggestions; check accuracy before use"), purpose, prompt, new HBox(8, ask, cancel),
                suggestion, new HBox(8, accept, discard), fresh); VBox.setVgrow(suggestion, Priority.ALWAYS);
        HBox content = new HBox(16, draft, assistance); draft.setPrefWidth(510); assistance.setPrefWidth(590);
        HBox.setHgrow(draft, Priority.ALWAYS); HBox.setHgrow(assistance, Priority.ALWAYS); VBox.setVgrow(content, Priority.ALWAYS);
        status = new Label(); status.setWrapText(true); status.setId("ai-status"); Label heading = new Label("Lesson AI Assistance"); guiTools.ASUTheme.stylePageTitle(heading);
        VBox root = new VBox(10, heading, content, status, back); root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 1150, 760); guiTools.ASUTheme.apply(scene); stage.setScene(scene); stage.setTitle("TP3: AI Assistance");
        stage.setOnCloseRequest(e -> ControllerAiAssistance.cancel()); ControllerAiAssistance.newConversation(); stage.show();
    }
    private static TextArea area(String id, boolean editable) { TextArea area = new TextArea(); area.setId(id); area.setWrapText(true); area.setEditable(editable); return area; }
}
