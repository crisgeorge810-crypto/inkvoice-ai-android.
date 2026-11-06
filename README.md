# InkVoice AI — Android starter project

This is the clean foundation for the new InkVoice AI app.

## Included in this first build
- General Notes and Church Mode selector
- Topic field and manual New topic control
- Android speech-to-text using the phone's available recognition service
- A spoken "New topic" phrase handler when recognized
- Local note saving
- Separate audio recording and playback for saved M4A files
- Simple animated pen indicator while speech recognition is listening

## Important current limitations
This is a starter milestone, not the complete product. Speech recognition and MediaRecorder are separate in this version; they do not record simultaneously. Offline recognition depends on whether the phone has an offline speech language pack installed and supported. The app does not yet include automatic topic AI, six-hour background recording service, live pen-stroke animation, external mixer routing validation, cloud backup, account backend, payments, or subscription/device binding. Those need subsequent implementation and testing.

## Build
Use the included GitHub Actions workflow in `.github/workflows/build.yml`.
