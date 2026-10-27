# TP2 Task 3 and Task 4 Javadoc Generation

This folder keeps the exact source selections and evidence checklist for the two assigned TP2
documentation tasks. The Java source files remain in their correct packages so the application
does not contain duplicate classes.

## Included helper files

- `task3_sources.txt` lists the persistent lesson entity classes.
- `task4_sources.txt` lists both Contributor MVC packages.
- `generate_javadocs.sh` generates the two required documentation sites with private attributes.

## Generate both folders on macOS

1. Check that this branch is active:

   ```bash
   git branch --show-current
   ```

   The result should be `tp2-javadocs`.

2. Locate the JavaFX SDK `lib` folder. It is the folder containing `javafx-controls.jar`.

3. Create the main TP2 submission folder if it does not already exist:

   ```bash
   mkdir -p "/Users/aruiz/Desktop/TP2"
   ```

4. From the repository root, run the helper. Replace the JavaFX path with the path on the current
   computer:

   ```bash
   chmod +x Javadoc_Generation/generate_javadocs.sh
   ./Javadoc_Generation/generate_javadocs.sh \
     "/path/to/javafx-sdk-25.0.4/lib" \
     "/Users/aruiz/Desktop/TP2"
   ```

   If `javadoc` is not on the shell path, pass it as the third argument:

   ```bash
   ./Javadoc_Generation/generate_javadocs.sh \
     "/path/to/javafx-sdk-25.0.4/lib" \
     "/Users/aruiz/Desktop/TP2" \
     "$(/usr/libexec/java_home)/bin/javadoc"
   ```

The helper creates:

```text
TP2/
├── Lessons_Learned_Javadocs/
│   ├── index.html
│   └── Screenshots/
└── Contributor_Javadocs/
    ├── index.html
    └── Screenshots/
```

Keep every generated HTML, CSS, JavaScript, index, and supporting file. Open both `index.html`
files and verify that the class links work before adding screenshots.

## Task 3 screenshot checklist

Place these images in `Lessons_Learned_Javadocs/Screenshots`:

1. `3.1_EntityClasses_Package.png` — Project Explorer showing `LessonsLearned`,
   `LessonsLearnedList`, `ExperienceRecord`, and `TeamMemberEffort` in `entityClasses`.
2. `3.2_LessonsLearned_CRUD.png` — `LessonsLearned` Create, Read, Update, and Delete methods.
3. `3.2_LessonsLearnedList_CRUD.png` — collection CRUD methods.
4. `3.3_Attributes_And_Rationale.png` — ID, owner, core information, and lock attributes with
   their Javadocs.
5. `3.3_Experience_Information.png` — time and team-effort entity attributes.
6. `3.4_Generated_Class_Javadocs.png` — readable generated class description and method summary.
7. `3.5_Internal_Documentation.png` — ownership, lock, filtering, or synchronization comments.

## Task 4 screenshot checklist

Place these images in `Contributor_Javadocs/Screenshots`:

1. `4.1_Contributor_Packages.png` — both Contributor packages expanded with every class visible.
2. `4.2_Lessons_MVC.png` — one list View action, its Controller workflow, and matching Model call.
3. `4.2_Experience_MVC.png` — one details View action, Controller workflow, and Model call.
4. `4.3_Lesson_GUI_Functionality.png` — list, filter, Create, View, Edit, and Delete controls.
5. `4.3_Experience_GUI_Functionality.png` — lesson details, lock status, and experience controls.
6. `4.4_GUI_Javadocs.png` — generated View, Controller, Model, and package documentation.
7. `4.5_Validation_And_Locks.png` — helpful errors, ownership, and locked-field documentation.

Use readable zoom and include a short caption for each image in the submission evidence.
