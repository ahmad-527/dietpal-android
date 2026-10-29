# Portfolio changes

This is an October 2026 portfolio edition of an undergraduate Mobile Application Development project. The original D-drive source and submitted report remain unchanged privately. These preparation changes and additional checks were made with Codex assistance; they are not represented as features or test evidence from the 2023–24 submission.

## Application fixes

- Successful login now lets MainActivity create its home fragment, removing a transaction against a container missing from the login layout. Registration and password verification run outside the UI thread. Logout clears the previous activity stack.
- Password storage uses random salts and PBKDF2-HMAC-SHA256 with 600,000 iterations. Database version 2 migrates legacy demo passwords without dropping the accounts. Cursors close after use; new databases enforce unique usernames. This does not create a hosted authentication service or constitute a production-security audit.
- BMI arithmetic rejects blank, non-numeric, non-finite, zero and negative inputs. The calculation is separated from the fragment for testing.
- Workouts validate their exercise ID before loading one of the fifteen layouts. Timers stop when the screen stops/destroys, and completed screens finish rather than accumulating in the back stack.
- Recipe configuration is loaded from ignored local properties into BuildConfig. The archived key is absent. Missing-key and error states close loading indicators; transport errors do not display request URLs. Requests are cancelled when recipe screens close.
- Informational pages describe actual local-data and optional API behaviour. The unfinished settings menu entry is hidden. Other retained placeholder source is not advertised as completed functionality.

## Resources and build

- Replaced all 34 original raster assets, including the fifteen workout GIFs, photos and raster launcher icons, after the owner stated their reuse permissions were unknown. The replacements are original Android XML vectors and gradients. Workout illustrations are static. Dark backgrounds preserve contrast with the existing white labels; vector intrinsic dimensions are bounded.
- Replaced GIF-specific image buttons with standard vector-capable views, refined navigation icons and added accessibility labels/decorative-image handling.
- Extracted 202 static UI strings into resources and changed 58 text sizes from dp to sp. English remains the supported content language.
- Kept the original application structure, Java implementation, package ID and main feature areas. Min SDK is now 26 for the password-hashing/Base64 APIs; compile/target SDK remains 34.
- Aligned the wrapper JAR with Gradle 8.0 and verified its official checksum; pinned the distribution checksum. Picasso changed from 2.71828 to AndroidX-compatible 2.8 after the original dependency introduced legacy support libraries. The unused GIF dependency was removed.
- Added meaningful JVM/Robolectric tests and an emulator workflow test. Removed the original template addition/package-name assertions from the reported checks.

The private preparation records bind the original file hashes, changes, APK and public file list. They contain internal paths and are excluded from this repository.
