package aiAssistance;

/*******
 * <p> Title: AiPurpose Enum. </p>
 * <p> Description: Allowed Contributor and Curator assistance tasks. Tested by
 * AiAssistanceTest.purposesAndPermissions and TP3AiGuiTestingAutomation.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 AI purposes
 */
public enum AiPurpose {
    /** Improve a Contributor draft. */ WRITE,
    /** Suggest questions, alternatives and missing context. */ BRAINSTORM,
    /** Retrieve examples with web search and show actual citation links. */ EXAMPLES,
    /** Refine a Curator-selected lesson. */ REFINE,
    /** Suggest related terms for explicit Curator review. */ TERMS
}
