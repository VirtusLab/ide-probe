### Debug mode
To debug the probe, set up the `IDEPROBE_DEBUG=true` environment variable.

#### Port 
By default, user can connect on the `5005` port. To change it, use  the `IDEPROBE_DEBUG_PORT` environment variable.

#### Suspend
By default, the IDE JVM will **not** wait for the user to connect. To force this behavior, set the `IDEPROBE_DEBUG_SUSPEND=true` environment variable.
