### Adding a server to the list

Mindustry now has a public list of servers that everyone can see and connect to. 
This is done by letting clients `GET` a [JSON list of servers](https://github.com/Anuken/Mindustry/blob/master/servers_v7.json) in this repository.

You may want to add your server to this list. The steps for getting this done are as follows:

0. **Take note of the fact that modded servers are not allowed on this list.** Such servers confuse users, and there's currently no easy way to fix mod incompatibilities after a failed connection.
1. **Ensure your server is properly moderated.** For the most part, this applies to survival servers, but PvP servers can be affected as well.
You'll need to either hire some moderators, or make use of (currently non-existent) anti-grief and anti-curse plugins.
*Consider enabling a rate limit:* `config messageRateLimit 2` will make it so that players can only send messages every 2 seconds, for example.
2. Make sure that your server is able to handle inappropriate content - this includes NSFW display/sorter art and abusive messages. **Servers that allow such content will be removed immediately.** Consider banning display blocks if it is a problem for your server: `rules add bannedBlocks ["canvas", "logic-display", "large-logic-display"]`.
3. **Set an appropriate MOTD, name and description.** This is set with `config <name/desc/motd> <value>`. "Appropriate" means that:
   - Your name or description must reflect the type of server you're hosting. 
   Since new players may be exposed to the server list early on, put in a phrase like "Co-op survival" or "PvP" so players know what they're getting into. Yes, this is also displayed in the server mode info text, but having extra info in the name doesn't hurt.
   - Make sure players know where to refer to for server support. It should be fairly clear that the server owner is not me, but you.
   - Try to be professional in your text; use common sense.
4. **Get some good maps.** *(optional, but highly recommended)*. Add some maps to your server and set the map rotation to custom-only. You can get maps from the Steam workshop by subscribing and exporting them; using the `#maps` channel on Discord is also an option.
5. **Check your server configuration.** *(optional)* I would recommend adding a message rate limit of 1 second (`config messageRateLimit 1`), and disabling connect/disconnect messages to reduce spam (`config showConnectMessages false`).
6. Finally, **submit a pull request** to add your server's IP to the list. 
This should be fairly straightforward: Press the edit button on the [server file](https://github.com/Anuken/Mindustry/blob/master/servers_v7.json), then add a JSON object with a single key, indicating your server address.
For example, if your server address is `example.com:6000`, you would add a comma after the last entry and insert:
    ```json
      {
        "address": "example.com:6000"
      }
    ```
    > Note that Mindustry also support SRV records. This allows you to use a subdomain for your server address instead of specifying the port. For example, if you want to use `play.example.com` instead of `example.com:6000`, in the dns settings of your domain, add an SRV record with `_mindustry` as the service, `tcp` as the protocol, `play` as the target and `6000` as the port. You can also setup fallback servers by modifying the weight or priority of the record. Although SRV records are very convenient, keep in mind they are slower than regular addresses. Avoid using them in the server list, but rather as an easy way to share your server address.

    Then, press the *'submit pull request'* button and I'll take a look at your server. If I have any issues with it, I'll let you know in the PR comments.
