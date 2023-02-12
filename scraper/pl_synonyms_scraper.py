from bs4 import BeautifulSoup
from urllib.request import urlopen

WORDS_FILE_NAME = "pl.txt"
RESULT_FILE_NAME = "polish_synonyms.txt"


def scrap_synonyms_of_word(word):
    result = []
    url = f"https://synonim.net/synonim/{word}"
    page = urlopen(url)
    html = page.read().decode("utf-8")
    soup = BeautifulSoup(html, "html.parser")
    mall = soup.find(id="mall")
    span = mall.find("span")
    ul = span.find("ul")
    lis = ul.select("li")
    for li in lis:
        text = li.find("a").text
        if len(text.split(" ")) == 1:
            result.append(text)
    return result


def write_result(word, synonyms):
    with open(RESULT_FILE_NAME, "a", encoding='utf-8') as f:
        f.write(f"{word}, {', '.join(synonyms)}\n")


def read_words():
    words = []
    with open(WORDS_FILE_NAME, "r", encoding='utf-8') as f:
        words = f.read().splitlines()
        index = words.index("allochtoniczny")
        words = words[index+1:]
    return words


def main():
    # open(RESULT_FILE_NAME, "w").close()

    words = read_words()
    for idx, word in enumerate(words):
        try:
            synonyms = scrap_synonyms_of_word(word)
            write_result(word, synonyms)
            print(f"{idx + 1} - Scrapped synonyms of {word}")
        except Exception as e:
            print(f"Exception for word: {word} -  {e}")


if __name__ == "__main__":
    main()
