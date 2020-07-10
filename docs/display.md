#### Display

To configure the display used by the IDE, user must specify the value of the environment variable `IDEPROBE_DISPLAY` to one of:
1. `native` - used by default, useful, when a user wants to see what happens in the IDE and interact with it during a single scenario. 
2. `xvfb` - requires the xvfb to be installed on the system - can be used to run tests on CI or just to disable the disrupting windows being open when used on a personal machine

Additionally, unless run in `headless` mode, the driver captures the screenshots of the screen periodically, so it can then be exposed on the CI for the user to inspect the IDE state in case of failures.