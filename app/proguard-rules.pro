# R8 rules for release builds. Libraries ship their own rules; these cover the rest.

# Credential Manager loads its Google Play services provider by reflection.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
