package entityClasses;

/*******
 * <p> Title: SearchTerm Class. </p>
 * <p> Description: One normalized related term and its parent lesson. Validated by TP3DiscoveryTest.termsNormalizationAndClear and persistenceAcrossProcesses.</p>
 * @author CSE 360 Team Project
 * @version 1.00 2026-11-19 Phase 3 discovery and Curator operations
 */
public class SearchTerm {
    /** Parent key binds each term to exactly one lesson. */
    private final long lessonId;
    /** Trimmed lower-case text, validated by the Curator service. */
    private final String term;
    /**
     * Tested by TP3DiscoveryTest.termsNormalizationAndClear.
     * @param lessonId parent lesson key
     * @param term normalized search text
     */
    public SearchTerm(long lessonId, String term) { this.lessonId = lessonId; this.term = term; }
    /** @return parent key; tested by termsNormalizationAndClear */
    public long getLessonId() { return lessonId; }
    /** @return saved normalized term; tested by termsNormalizationAndClear */
    public String getTerm() { return term; }
}
