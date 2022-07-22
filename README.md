# SelfhostedYggdrasil
#### This is a basic example how to self-host a Minecraft session server instance. Based on a minimal invasive approach still supporting a fully unmodified vanilla server jar. Also mixed official and invalid sessions are supported.

This approach is based on bending the DNS record sessionserver.mojang.com on the localhost via hosts file. Valid migrated Mojang accounts can still join, because requests without the new additional authString= GET parameter are forwarded to the actual Mojang session service. 

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





