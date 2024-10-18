# Random world names

Generates random world names for the world creation screen. Just `New World` gets boring after a while, doesn't it?

The default list of names allows for over 10 billion different combinations.
There are only two cases the default name is used:
- Either your computer is too slow to generate a new name within the pre-set time limit (2 seconds)
- Or you have so many worlds that no unique combinations could be found

### Using your own names

Just create a resourcepack and put a file at `assets/random-world-names/names.json`
and fill your names into a json array:
```json
[
    "name1",
    "name2",
    "name3",
    ...
]
```
That's it!


### License

This mod is licensed under the LGPL-3.0 (or any later version) license.
For details, please see the included [License file](https://github.com/moehreag/random-world-names/blob/main/LICENSE).
