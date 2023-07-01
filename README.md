# Boiler
Extension Plugin for MapEngine that allows you to show different types of content on a map display

Download:
https://modrinth.com/plugin/boiler

## Dependencies
These Plugins are required to use Boiler
- https://modrinth.com/plugin/mapengine
- https://modrinth.com/plugin/mapengine-mediaext

## Using the plugin
### creating a new screen
`/boiler create <x1> <y1> <z1> <x2> <y2> <z2>`

### deleting a existing screen
`/boiler delete <id>`

### listing all screens
`/boiler list`

### identify a screen by looking at it
`/boiler identify`

### setting sources
`/boiler setsource <source> <id> [args]`

#### List of avaiable sources
##### image
Just a simple image. Example:
`/boiler setsource image 1 https://example.com/image.png`

##### gif
An animated gif. Example:
`/boiler setsource gif 1 https://example.com/image.gif`

##### whiteboard
A board on which you can draw by clicking it
`/boiler setsource whiteboard 1`

##### file
Play local mp4 files
`/boiler setsource file 1 video.mp4`
**Note:** Videos can be placed in the media folder inside the plugins data folder

##### web
Stream to a screen using the integrated website
`/boiler setsource web 1`

**Note:**
The port of the webserver can be configured in the plugin config (default is 7011). You also need to set up a reverse proxy like nginx to put a valid ssl certificate in front of the webserver. Otherwise your browser cannot acces any camera.
