package entityClasses;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import database.Database;
import lessonManagement.LessonDraft;
import lessonManagement.OperationResult;

/*******
 * <p> Title: LessonsLearnedList Class. </p>
 *
 * <p> Description: Holds the persistent list of lessons or a keyword-selected subset and
 * provides collection-level Create, Read, Update, and Delete operations. Contributor-facing
 * lists are loaded by owner, while {@link #readAll(Database)} is reserved for trusted system
 * workflows. An ArrayList is used so empty, one-item, and growing lists have no fixed limit.</p>
 *
 * <p>The keywords used to produce a subset are retained. After a successful create, update, or
 * delete, the in-memory list is changed immediately and the same keyword rules are reapplied.
 * This prevents stale or duplicate entries in both complete lists and subsets.</p>
 *
 * @author CSE 360 Team Project
 *
 * @version 1.00        2026-10-01 Initial HW2 version
 * @version 2.00        2026-10-22 Added persistent CRUD and keyword subsets for TP2
 */
public class LessonsLearnedList {

	/** Shared, dynamically sized backing data for a full list and all of its live subsets. */
	private final List<LessonsLearned> lessons;

	/** Normalized title keywords that determine which backing records this subset displays. */
	private final List<String> keywords;

	/** Construct an empty collection. */
	public LessonsLearnedList() {
		lessons = new ArrayList<LessonsLearned>();
		keywords = new ArrayList<String>();
	}

	/**
	 * @param source specifies the lessons to copy into this collection
	 */
	public LessonsLearnedList(List<LessonsLearned> source) {
		this(source, Collections.emptyList(), false);
	}

	private LessonsLearnedList(List<LessonsLearned> source, List<String> searchKeywords,
			boolean shareSource) {
		lessons = source == null ? new ArrayList<LessonsLearned>() :
				shareSource ? source : new ArrayList<LessonsLearned>(source);
		keywords = new ArrayList<String>(searchKeywords);
	}

	/**********
	 * <p> Method: readAll(Database database) </p>
	 *
	 * <p> Description: Read every persisted lesson for trusted system-level workflows. The
	 * Contributor GUI must use {@link #readForContributor(Database, String)} instead.</p>
	 *
	 * @param database specifies the Foundations database used for persistence
	 * @return every stored lesson or a helpful database error
	 */
	public static OperationResult<LessonsLearnedList> readAll(Database database) {
		try {
			LessonsLearnedList result = new LessonsLearnedList(database.getAllLessons());
			return OperationResult.success(result, messageFor(result));
		} catch (SQLException e) {
			return OperationResult.failure("The lesson list could not be loaded. Try again.");
		}
	}

	/**********
	 * <p> Method: readForContributor(Database database, String contributor) </p>
	 *
	 * <p> Description: Read only the lessons owned by the signed-in contributor. The owner
	 * restriction is also applied by the database query so other submissions never enter this
	 * list.</p>
	 *
	 * @param database specifies the Foundations database used for persistence
	 * @param contributor specifies the signed-in contributor
	 * @return the contributor's complete lesson list or a helpful error
	 */
	public static OperationResult<LessonsLearnedList> readForContributor(Database database,
			String contributor) {
		if (blank(contributor))
			return OperationResult.failure("Sign in as a contributor to view lessons.");
		try {
			LessonsLearnedList result = new LessonsLearnedList(
					database.getLessonsByOwner(contributor));
			return OperationResult.success(result, messageFor(result));
		} catch (SQLException e) {
			return OperationResult.failure("The lesson list could not be loaded. Try again.");
		}
	}

	/** @return an unmodifiable view of the current lesson list */
	public List<LessonsLearned> getLessons() {
		return Collections.unmodifiableList(visibleLessons());
	}

	/** @return the normalized title keywords that define this subset */
	public List<String> getKeywords() {
		return Collections.unmodifiableList(keywords);
	}

	/** @return the number of lessons in this collection */
	public int size() { return visibleLessons().size(); }

	/** @return true when this collection contains no lessons */
	public boolean isEmpty() { return visibleLessons().isEmpty(); }

	/**********
	 * <p> Method: findById(long id) </p>
	 *
	 * @param id specifies the lesson to locate
	 * @return the matching lesson, or null when it is not present
	 */
	public LessonsLearned findById(long id) {
		for (LessonsLearned lesson : visibleLessons())
			if (lesson.getId() == id) return lesson;
		return null;
	}

	/**********
	 * <p> Method: create(Database database, String contributor, LessonDraft draft) </p>
	 *
	 * <p> Description: Persist a new lesson and add it to this list when it matches the list's
	 * current keyword selection.</p>
	 *
	 * @param database specifies the Foundations database used for persistence
	 * @param contributor specifies the signed-in contributor and new lesson owner
	 * @param draft specifies the proposed lesson information
	 * @return the created lesson or a helpful error
	 */
	public OperationResult<LessonsLearned> create(Database database, String contributor,
			LessonDraft draft) {
		OperationResult<LessonsLearned> result = LessonsLearned.create(database, contributor,
				draft);
		if (result.isSuccessful()) addOrReplace(result.getValue());
		return result;
	}

	/**********
	 * <p> Method: read(Database database, long id, String contributor) </p>
	 *
	 * @param database specifies the Foundations database used for persistence
	 * @param id specifies the selected lesson identifier
	 * @param contributor specifies the signed-in contributor
	 * @return the current persisted lesson or a helpful error
	 */
	public OperationResult<LessonsLearned> read(Database database, long id, String contributor) {
		return LessonsLearned.read(database, id, contributor);
	}

	/**********
	 * <p> Method: update(Database database, long id, String contributor, LessonDraft draft) </p>
	 *
	 * <p> Description: Update one lesson and then add, replace, or remove its list entry according
	 * to the current keyword selection.</p>
	 *
	 * @param database specifies the Foundations database used for persistence
	 * @param id specifies the selected lesson identifier
	 * @param contributor specifies the signed-in contributor
	 * @param draft specifies the proposed replacement values
	 * @return the updated lesson or a helpful error
	 */
	public OperationResult<LessonsLearned> update(Database database, long id,
			String contributor, LessonDraft draft) {
		try {
			LessonsLearned existing = database.getLesson(id);
			if (existing == null)
				return OperationResult.failure("Lesson not found. Refresh the list.");
			OperationResult<LessonsLearned> result = existing.update(database, contributor, draft);
			if (result.isSuccessful()) addOrReplace(result.getValue());
			return result;
		} catch (SQLException e) {
			return OperationResult.failure("The lesson could not be updated. Try again.");
		}
	}

	/**********
	 * <p> Method: delete(Database database, long id, String contributor) </p>
	 *
	 * <p> Description: Delete one permitted lesson and remove any matching entry from this list.</p>
	 *
	 * @param database specifies the Foundations database used for persistence
	 * @param id specifies the selected lesson identifier
	 * @param contributor specifies the signed-in contributor
	 * @return a successful result or a helpful error
	 */
	public OperationResult<Boolean> delete(Database database, long id, String contributor) {
		try {
			LessonsLearned existing = database.getLesson(id);
			if (existing == null)
				return OperationResult.failure("Lesson not found. Refresh the list.");
			OperationResult<Boolean> result = existing.delete(database, contributor);
			if (result.isSuccessful()) lessons.removeIf(lesson -> lesson.getId() == id);
			return result;
		} catch (SQLException e) {
			return OperationResult.failure("The lesson could not be deleted. Try again.");
		}
	}

	/**********
	 * <p> Method: filterByTitle(String searchText) </p>
	 *
	 * <p> Description: Build a case-insensitive title subset.  A blank search returns a copy of
	 * the full collection.</p>
	 *
	 * @param searchText specifies the title text to match
	 * @return a new collection containing only matching lessons
	 */
	public LessonsLearnedList filterByTitle(String searchText) {
		return filterByKeywords(searchText);
	}

	/**********
	 * <p> Method: filterByKeywords(String searchText) </p>
	 *
	 * <p> Description: Build a case-insensitive title subset. Every whitespace-separated keyword
	 * must appear in the title, in any order. A blank search returns a complete copy.</p>
	 *
	 * @param searchText specifies one keyword or a whitespace-separated set of keywords
	 * @return a new list containing only lessons that match every keyword
	 */
	public LessonsLearnedList filterByKeywords(String searchText) {
		List<String> requestedKeywords = normalizeKeywords(searchText);
		List<String> combinedKeywords = new ArrayList<String>(keywords);
		for (String keyword : requestedKeywords)
			if (!combinedKeywords.contains(keyword)) combinedKeywords.add(keyword);
		return new LessonsLearnedList(lessons, combinedKeywords, true);
	}

	private void addOrReplace(LessonsLearned updated) {
		lessons.removeIf(lesson -> lesson.getId() == updated.getId());
		lessons.add(updated);
		lessons.sort(Comparator.comparingLong(LessonsLearned::getId));
	}

	private List<LessonsLearned> visibleLessons() {
		List<LessonsLearned> result = new ArrayList<LessonsLearned>();
		for (LessonsLearned lesson : lessons)
			if (matches(lesson, keywords)) result.add(lesson);
		return result;
	}

	private static boolean matches(LessonsLearned lesson, List<String> requestedKeywords) {
		String title = lesson.getTitle().toLowerCase(Locale.ROOT);
		for (String keyword : requestedKeywords)
			if (!title.contains(keyword)) return false;
		return true;
	}

	private static List<String> normalizeKeywords(String searchText) {
		List<String> result = new ArrayList<String>();
		if (searchText == null || searchText.isBlank()) return result;
		for (String keyword : searchText.trim().toLowerCase(Locale.ROOT).split("\\s+"))
			if (!keyword.isBlank() && !result.contains(keyword)) result.add(keyword);
		return result;
	}

	private static String messageFor(LessonsLearnedList lessons) {
		return lessons.isEmpty() ? "No lessons found." :
				lessons.size() + " lesson(s) found.";
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
