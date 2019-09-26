## Translating for Mindustry

To begin, log in to your GitHub account, or if you don't have one yet, create it [here](https://github.com/).

Consult [this list](https://www.science.co.il/language/Locale-codes.php) to find the locale code for your language. Once you've found it, 
head over to the translation bundle folder and check the [list of bundles](https://github.com/Anuken/Mindustry/tree/master/core/assets/bundles) that have already been created. 
You're looking for a file called "`bundle_`(insert locale code here)`.properties`". If you don't find one, create one manually (more info below).

#### Editing an existing translation

If a translation bundle already exists, that means someone has already started working on a translation. To edit it or translate text, simply click the file and press the edit (pencil) button in the top right. Once you're done editing, press the green "propose file change" button at the bottom, then "create pull request" (twice).
Once this is done, all you need to do is wait for me to approve your changes.

#### Creating a new translation bundle

If a translation bundle for your language *doesn't* exist, you need to create one yourself.  
In the folder with all the bundles in it, click the *'create new file'* button, and name it `bundle_(locale code here).properties`. 
Then, copy-paste the entire contents of the [English translation bundle](https://raw.githubusercontent.com/Anuken/Mindustry/master/core/assets/bundles/bundle.properties) into the file, and translate all the necessary text to your language.
Once you are done, press the *propose new file* button at the bottom, then 'create pull request' twice.  

#### Useful Information

- When you see text surrounded by square brackets, such as `[RED]`, `[]` or `[accent]`, this indicates a color code. Don't translate it.
- `{0}` means an argument that will be replaced when the text is displayed. For example, `Wave: {0}` will replace the `{0}` with whatever wave you are in.
- Empty lines are fine, and it doesn't matter in what order you place the text.
- `\n` means "new line". If you want to split text into multiple lines, use `\n` to do it.

#### Testing your translation bundle

There are two ways to test the translation bundle:
1) Assuming you have the PC version downloaded, download your bundle file, name it `bundle.properties`, then place it in the same folder as the Mindustry desktop executable and run it. *You should get a popup message in-game confirming that you have loaded an external translation.*
2) For advanced users: simply download your fork of mindustry and compile/run the game.

#### Translating for stores (Steam, Google Play)

If you would like to translate the descriptions for Google Play or Steam, see the [Fastlane Metadata folder](https://github.com/Anuken/Mindustry/tree/master/fastlane/metadata) and submit a pull request for files there. On Google Play, you would create or edit the folder with the correct local code; for Steam, I have to update the translations manually, so just name the folder with the language name, and include the same files as the English folder does.

**And that's it.**  

*(...of course, that's never really it. Bother me on Discord when something inevitably goes wrong.)*
