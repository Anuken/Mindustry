### Adding a server to the list

Mindustry now has a public list of servers that everyone can see and connect to. 
This is done by letting clients `GET` a [JSON list of servers](https://github.com/Anuken/Mindustry/blob/master/servers.json) in this repository.

You may want to add your server to this list. The steps for getting this done are as follows:

1. **Ensure your server is properly moderated.** For the most part, this applies to survival servers, but PvP servers can be affected as well.
You'll need to either hire some moderators, or make use of (currently non-existent) anti-grief and anti-curse plugins. 
*Consider enabling a rate limit:* `config messageRateLimit 2` will make it so that players can only send messages every 2 seconds, for example.
2. **Set an appropriate MOTD, name and description.** This is set with `config <name/desc/motd> <value>`. "Appropriate" means that:
  - Your name or description must reflect the type of server you're hosting. 
  Since new players may be exposed to the server list early on, put in a phrase like "Co-op survival" or "PvP" so players know what they're getting into. Yes, this is also displayed in the server mode info text, but having extra info in the name doesn't hurt.
  - Make sure players know where to refer to for server support. It should be fairly clear that the server owner is not me, but you.
  - Try to be professional in your text; use common sense.
3. **Get some good maps.** *(optional, but highly recommended)*. Add some maps to your server and set the map rotation to custom-only. You can get maps from the Steam workshop by subscribing and exporting them; using the `#maps` channel on Discord is also an option.
4. **Check your server configuration.** *(optional)* I would recommend adding a message rate limit of 1 second (`config messageRateLimit 1`), and disabling connect/disconnect messages to reduce spam (`config showConnectMessages false`).
5. Finally, **submit a pull request** to add your server's IP to the list. 
This should be fairly straightforward: Press the edit button on the [server file](https://github.com/Anuken/Mindustry/blob/master/servers.json), then add a JSON object with a single key, indicating your server address.
For example, if your server address is `google.com`, you would add a comma after the last entry and insert:
```json
  {
    "address": "google.com"
  }
```
Then, press the *'submit pull request'* button and I'll take a look at your server. If I have any issues with it, I'll let you know in the PR comments.
