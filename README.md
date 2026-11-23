# agent.droid
Autonomous AI agent platform for Android

## Design decisions

- [ADR 0001: How the agent acts on the device](docs/adr/0001-device-action-model.md) — AccessibilityService as the primary capability path, ADB self-pairing optional, root out of scope.

## Running the device smoke test

GitHub-hosted runners cannot install and launch the app: the emulator needs
hardware virtualisation, which those runners do not expose. That check is a
separate `device-smoke` job that installs the debug APK and asserts the app
process is alive, `MainActivity` is the resumed activity, and the crash buffer
is clean. It only runs on manual dispatch, on a self-hosted runner labelled
`kvm`, so ordinary pushes stay green.

1. On a Linux host with VT-x/AMD-V enabled, give the runner user access to KVM:

   ```bash
   sudo usermod -aG kvm "$USER"   # log out and back in
   echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' \
     | sudo tee /etc/udev/rules.d/99-kvm4all.rules
   sudo udevadm control --reload-rules && sudo udevadm trigger --name-match=kvm
   ```

2. Register the runner for this repository with the extra `kvm` label (Settings
   → Actions → Runners → New self-hosted runner provides the token):

   ```bash
   ./config.sh --url https://github.com/mister365143/agent.droid \
     --token <TOKEN> --labels self-hosted,linux,x64,kvm
   sudo ./svc.sh install && sudo ./svc.sh start
   ```

   The JDK, Android SDK, system image and Gradle come from the workflow itself,
   so nothing else needs installing.

3. Actions → Android CI → Run workflow. The job stops immediately with an
   explanatory error if `/dev/kvm` is missing or not writable by the runner
   user, instead of timing out during the emulator boot.
