# Standalone New Tab Page (NTP)

- In the context of: `changing the Chromium NTP to add Ecosia features and branding`

- facing: `the requirement to iterate on these frequently and stay able to run Chromium upgrades`

- we decided: `to create a new Standalone NTP (which is a different file and class)`

- and neglected: `the option of incoorporating our changes into the original NTP from Chromium`

- to achieve: `lower iteration effort; full control over code, layout and features; fewer merge conflicts on upgrades; and the absence of Chrome's experiments or other undesired changes (like Tab Groups)`

- accepting that: `we miss out on potential layout fixes by Chromium; we diverge from the Chromium NTP; we might face breaking changes that require us to refactor or rebuild our own NTP; we face changes to internal data APIs (e.g. Top Sites)`

## Related Code

- [EcosiaNewTabPage.java](../../chrome/android/java/src/org/ecosia/ntp/EcosiaNewTabPage.java)
- [NativePageFactory.java](../../chrome/android/java/src/org/chromium/chrome/browser/native_page/NativePageFactory.java#L90)
