## Social-Discord

A single file java service that scraps Facebook posts and mirrors them to Discord using webhooks... that's it. It was in *"production"* for a couple of years until FB started banning bots, after that I took it down permanently.

### Features

Supports:
 - Text posts.
 - Posts with a single image.
 - Album posts.
 - Translation using Google.

Everything is properly packed inside an embed. Auto splits into fields if source text is longer.

### Example and how it works

![image](https://user-images.githubusercontent.com/9023392/158050906-ebec9588-2be4-4010-9f22-fa9d2c674711.png)

It works by scraping the JS-less mobile FB page. It will list all the posts, scrap and check the id, then decode the text content and check for any extra images. For each image it will open their page and scrap the HQ link for use in the embeds. After that it will cleanup the text and make it Discord compatible. Finally put everything together into a hand-made JSON text.

The colored strip is randomized to differentiate between messages.

In its original configuration it would check every 5 minutes but after FB started blocking scrappers I had to add more checks and increase the wait time to at least 30 minutes.

Configuration is stored like this:

 - `/config/`
 - `/config/hooks.json` JSON array with all target hook urls.
 - `/config/page.txt` Single line with the page name.
 - `/config/report.txt` Single line hook url to which it would send a message when it gets banned.

`/` Is the work directory.

### History

I originally wrote this service to mirror Honkai Impact 3rd's Facebook feed into our armada Discord.

It was originally called `fb-discord` but then decided that it would be a good idea to add more sources, Twitter for example, I changed the name to the current one but that never ended up happening.

Code quality is bad, this is me being as lazy as possible while getting away with it.

I'm only making it public for archival purposes. If FB's original JS-less page is still up it should work as-is tho I have not tested it since then.
