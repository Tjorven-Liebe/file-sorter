📂 Metadata Folder Organizer

A powerful Java Swing application that automatically organizes unorganized files into a clean, hierarchical folder structure based on their internal metadata (ID3 tags, PDF properties, Document info, etc.).
✨ Features

    Recursive Sorting: Create multi-level directory structures (e.g., Artist > Album > Genre > File).

    Deep File Analysis: Uses Apache Tika to extract metadata from hundreds of file types (MP3, PDF, DOCX, JPG, etc.).

    Live Preview: See exactly how your files will be moved before committing to the operation.

    Safety Revert: Made a mistake? Use the Revert function to move all files back to their original locations and delete the empty directories created.

    Performance Optimized: Uses parallel streams and NIO.2 for fast I/O operations and efficient memory management.

    Smart Sanitization: Automatically cleans metadata values to ensure they are valid folder names across all operating systems.

🛠️ Technical Stack

    Language: Java 17+

    UI Framework: Swing (Nio.2 for File Handling)

    Metadata Engine: Apache Tika

    Boilerplate: Lombok

🚀 Getting Started
Prerequisites

    Java JDK 17 or higher.

    Maven (for dependency management).

Installation

    Clone the repository:
    Bash

git clone https://github.com/yourusername/metadata-folder-organizer.git

Build the project:
Bash

mvn clean install

Run the application:
Bash

    java -jar target/metadata-organizer-1.0.jar

📖 How to Use

    Select Folder: Pick the directory containing your unorganized files.

    Define Hierarchy: * Select a metadata attribute from the dropdown (e.g., Author).

        Click Add Level.

        Repeat to create deeper subfolders (e.g., adding Album after Author).

    Preview: Check the "Move Preview" list to verify the path logic:

        song.mp3 -> [Daft Punk]/[Discovery]/song.mp3

    Run: Click Run Recursive Sort to organize the files.

    Undo: If the results aren't what you expected, click Revert Last Sort immediately to restore the original state.

📝 Blacklist Feature

The application includes a blacklist.txt file. You can add metadata keys to this file (e.g., X-Parsed-By) to hide technical or irrelevant metadata from the sorting options, keeping the UI clean.
🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

Would you like me to add a "Troubleshooting" or "Screenshots" section to this README?
