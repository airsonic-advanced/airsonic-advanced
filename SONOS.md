Initial Credits:
https://github.com/simojenki/airsonic-docs/blob/master/configure/sonos.md

# Sonos support
Airsonic supports 2 methods for integrating with Sonos.  **Multi user** and **One user**.
- **Multi user** allows linking one or multiple Airsonic accounts with Sonos, for example in family setting.  Play counts, history etc will be associated with the linked user.
- **One user** could be considered similar to anonymous access, all access and play counts etc will be associated with an anonymous user: 'anonymous'

There are 2 steps:
1. Enable Sonos in Airsonic
2. Adding music service in Sonos

### 1. Enabling Sonos support in airsonic
In Airsonic / Settings / Sonos
- Enable Sonos
- Select the method you wish to use to link Airsonic with Sonos, **Multi User** or **One User**
- Enter the Root host address, this is the network address of the Airsonic installation, and allows Sonos to communicate with Airsonic.  If your network has DNS it may be a DNS name, ie. <http://airsonic:8080/>, if your network does not have DNS then it must be the IP address, ie. <http://192.168.0.10:8080/>.  If you are using docker, please take into consideration the section on docker below. This setting **must** match the root of the EndPoint URL in the Sonos service config down below.
- Enter the Music service name. This is the name of your Airsonic service within the Sonos application.  ie. *Airsonic*
- Save

At this point Airsonic will try and automatically register itself with your Sonos players.  If successful you should see a message *Success of Airsonic service registration.*.  

If you do not see this message, and it is the first time you have enabled Sonos, you may need to wait a minute or so, before Saving again.  This is due to the fact that it takes some time when first enabled for Airsonic to discover your Sonos players in order to register the service, you need to successfully save to have Airsonic registered with the Sonos players.

If this is still not successfull it may be because Airsonic cannot find your Sonos players for some reason.  ie. Airsonic is running in docker, Airsonic is on a different network to your Sonos players.  See [Manually registering Airsonic with Sonos](#manually-registering-airsonic-with-sonos) below.

### 2. Adding the music service in the Sonos app.
At this point you need to add the music service in the Sonos app, this process may change over time as Sonos change their software, however the principle is as follows;
- Add a Service
- Choose your Airsonic service
- Add to Sonos
- If you chose **One user** method then Sonos and Airsonic should now be linked.
- If you chose **Multi user** method you need to Authorize the Sonos application with Airsonic, this will require logging into Airsonic when prompted, you should then see a messsage from Airsonic stating *Sonos successfully linked*

## Using Airsonic inside a docker container (or on a different network)
There are 2 additional considerations when connecting Sonos to Airsonic that is running within a docker container.

### How Sonos will connect to Airsonic
The Sonos players need to be able to contact the Airsonic installation over http, this is the purpose of the *Root host address* setting we configured previously.  As such when running Airsonic within docker you will need to use a *Root host address* for host that the container is running on.  There are multiple ways of configuring docker networking, however the simplest in this case is probably just to expose the port that Airsonic is running on. <https://docs.docker.com/engine/reference/run/#expose-incoming-ports>

### How Airsonic will register with the Sonos players.
There are multiple ways of configuring docker networking which go well beyond what we can cover here.  However, the following should provide enough information to get most people running.

The default docker network configuration is to use a 'bridge' network.  That is, it bridges a private internal docker created network, with the network that the host machine is attached to.  In this situation your docker container likely has a IP address something like 172.17.0.2/16, this will not be the same network as your Sonos player, so Airsonic cannot auto-discover your Sonos players in order to register.  See [Manually registering Airsonic with Sonos](#manually-registering-airsonic-with-sonos) below.

## Manually registering Airsonic with Sonos
You need to find the IP address of one of your Sonos players.  The easiest way to do this is within the Sonos app itself.

Settings / System / About my System / Sonos System Info

Choose an IP address of a player within the list.

Within an internet browser open the following URL, ensuring you replace the IP address with that of one of your Sonos players identified above.

<http://IP-ADDRESS-OF-A-SONOS-PLAYER:1400/customsd.htm>

You should see a very basic form for entering information.

Enter the following values into the form and submit.

- SID – Any legal value except 244, ie. 242
- Service Name – *Airsonic*
- Endpoint URL – http://[AIRSONIC-OR-DOCKER-HOST-IP]:[AIRSONIC-OR-DOCKER-HOST-PORT]/ws/Sonos  (Ensure the 'Root host address' config item on the Sonos settings page in Airsonic matches the root of this URL. The port may be 8080 for the default airsonic war or 4040 for the default docker container)
- Secure Endpoint URL – http://[AIRSONIC-OR-DOCKER-HOST-IP]:[AIRSONIC-OR-DOCKER-HOST-PORT]/ws/Sonos
- Polling Interval – 1200
- Authentication - *Anonymous* or *Application Link*.  If you chose **Multi User** in Airsonic, then use *Application Link*.  If you chose **One User** in Airsonic, then use *Anonymous*.
- Strings Table – Version: 11, URI: http://[AIRSONIC-OR-DOCKER-HOST-IP]:[AIRSONIC-OR-DOCKER-HOST-PORT]/sonos/strings.xml
- Presentation Map – Version: 1, URI: http://[AIRSONIC-OR-DOCKER-HOST-IP]:[AIRSONIC-OR-DOCKER-HOST-PORT]/sonos/presentationMap.xml
- Container Type – Music Service
- Capabilities
  - Search
  - Favorites: Adding/Removing Tracks
  - Favorites: Adding/Removing Albums
  - User Content Playlists
  - Extended Metadata

Submit.

You should now see the message 'Success' from the Sonos player.

Complete registration using the Sonos app as per [Adding the music service in the Sonos app](#2-adding-the-music-service-in-the-sonos-app) above.

## Troubleshooting.
### Unable to browse music when linked **One User/Anonymous** link method.
Addressed here: https://github.com/airsonic-advanced/airsonic-advanced/issues/299#issuecomment-640160127

It's not a bug, it's a deliberate security feature.

An anonymous user is not able to browse through music because they shouldn't by definition have any permissions: they're anonymous; they're unverified, unknown, untrusted. If they have access to everything, then there's no point of security. They should only have access to things that are public (and therefore have been explicitly made available to everyone, including anonymous).

Thus, they are able to browse public stuff (like, for instance, if you make your playlist public, they should be able to see it), but have no rights to see non-public stuff.

If you'd like to be able to browse all your non-public music, either use one of your existing accounts or create a new user account in airsonic-advanced for the explicit purpose of linking with sonos (like an account named "sonos").

### Other issues
Check the status of your link in Settings->Sonos. Check logs for exceptions.
