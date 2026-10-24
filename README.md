# SelfhostedYggdrasil

## Reference client mod (Minecraft 1.12.2 / Cleanroom / Java 25)

This is the reference Minecraft 1.12.2 client implementation for the self-hosted
Yggdrasil webserver. It targets **Cleanroom 0.7.12-alpha on Java 25**; it is not a
Java 8 Forge mod. The mod adds `authString` to the JSON body of the
`/session/minecraft/join` request (the backend reads `$json_data['authString']`)
and can override the local session identity.

The PHP webserver backend and `nginx.conf` with the files in `webroot` are an example.
This mod replaces the manual client-side Mixin described in the legacy client
setup below; the PHP, nginx, and server-side DNS instructions remain the same.

Build and run:

```sh
nix develop
./gradlew build
./gradlew runClient
```

Install `build/libs/selfhostedyggdrasil-1.0.2.jar` in the client's `mods`
directory. Configure `config/selfhostedyggdrasil.cfg`:

- `enabled` — enable the mod.
- `authString` — shared secret for this player; it must match the value for the
  player's UUID in the server's `webroot/accounts.json`. Keep it secret.
- `joinURL` — complete join endpoint exposed by the webserver, for example
  `https://<host>/session/minecraft/join`.
- `username`, `uuid` — optional fixed session identity; leave empty to keep the
  existing session.

The server's `hasJoined` request must still reach the webserver at
`sessionserver.mojang.com` through the nginx/DNS setup below. If `joinURL` uses
the self-signed certificate from that setup, trust the certificate in the client
JVM.

## Webserver setup (PHP backend)

#### This is a basic example how to self-host a Minecraft session server instance. Based on a minimal invasive approach still supporting a fully unmodified vanilla server jar. Also mixed official and invalid sessions are supported.

This approach is based on bending the DNS record sessionserver.mojang.com on the localhost via hosts file. Valid migrated Mojang accounts can still join, because requests without the new additional authString= GET parameter are forwarded to the actual Mojang session service. 

> **PHP requirements:** The provided `webroot/join.php` and
> `webroot/hasJoined.php` use PHP's built-in
> `dns_get_record('sessionserver.mojang.com.', DNS_A)` to look up Mojang's
> session server, and cURL for the HTTP requests. Install the PHP cURL extension
> (Debian/Ubuntu: `php-curl`); the PEAR `Net_DNS2` package (`php-net-dns2`) is no
> longer required.
>
> `dns_get_record()` uses the system-configured DNS resolver and does not read
> `/etc/hosts`. That resolver must return Mojang's real public
> `sessionserver.mojang.com` address. If split-horizon or local DNS maps that
> name to this self-hosted backend instead, the PHP proxy requests will fail.
> The `/etc/hosts` entry below is still required for the Minecraft server's Java
> process; it does not affect the PHP lookup.

Basic server setup manual:

1. Generate a self-signed certificate for the domain sessionserver.mojang.com
2. Add the certificate to the servers java keystore `sudo keytool -importcert -keystore /etc/ssl/certs/java/cacerts -storepass changeit -file SelfhostedYggdrasil.crt`
3. Configure nginx to serve requests under the host name sessionserver.mojang.com. Set the external path /session/minecraft/ to point to an accessible folder for www-data. Copy the files from webroot there
4. Forward the paths `^\/(blockedservers|session\/minecraft\/profile\/.+)$` in nginx to the actual Mojang services. Because we configure a hosts entry for this domain you need to explicitly set a public DNS resolver for those proxy forwards. Use a variable for the hostname to prevent caching
5. Block opening the JSON data files in the webroot or move them somewhere else, so they aren't accessible from the public
6. For an example nginx config see nginx.conf
7. Add an entry to the end of your /etc/hosts file pointing sessionserver.mojang.com to the local nginx instance `::1 sessionserver.mojang.com`
8. Set the authString for each unofficial player who should be able to join in accounts.json
9. Restart the Minecraft server to update DNS and certificate caches 

Client setup:
1. Add the previously generated public certificate to the java keystore
2. Set a local CNAME DNS record sessionserver.mojang.com -> yourserver.com
3. Install a Mixin to add the `?authString=xxx` parameter to the /session/minecraft/join HTTP requests inside YggdrasilMinecraftSessionService
4. For an example mixin implementation see [CutelessMod](https://github.com/Nessiesson/CutelessMod/blob/master/src/main/java/net/dugged/cutelessmod/mixins/MixinYggdrasilMinecraftSessionService.java)
5. Try joining the server, you should be able to join with migrated and unsupported legacy accounts if the authString between client and server matches

The authString serves as a new client authentication property to continue using servers in online mode without allowing everyone to join. The string should be kept a secret to prevent other people impersonating on other accounts.

### How does it work?

Usually the client sends the following JSON to `https://sessionserver.mojang.com/session/minecraft/join`

`{
    "accessToken": "<accessToken>",
    "selectedProfile": "<player's uuid without dashes>",
    "serverId": "<serverHash>"
    "authString": "<new shared secret string>"
}`

We add the new parameter authString with a shared secret string for authentication. If the server receives this, it sets the session entry in sessions.json to be valid for 60s for the given serverId hash.

When the server then sends his request to `https://sessionserver.mojang.com/session/minecraft/hasJoined?username=username&serverId=hash` php checks if there's a valid session entry less than 60s old. If the client performed a request previously your allowed to join and the server gets the usual response data from the `https://sessionserver.mojang.com/session/minecraft/profile/<uuid>` endpoint.

If the new parameter isn't available php requests original sessions from Mojang to be compatible with a mixed client group.

