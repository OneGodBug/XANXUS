#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from pathlib import Path
from xml.etree import ElementTree

ROOT_PATH = Path(__file__).parent.parent


def get_strings_names(path: Path):
    root = ElementTree.parse(path).getroot()
    return [
        element.get("name")
        for element in root.iter()
        if element.get("translatable") != "false"
    ]


def sort_strings_xml(input_path: Path, names: list[str]):
    tree = ElementTree.parse(input_path)
    root = tree.getroot()
    for key in set(names) - set(elem.get("name") for elem in root.iter()):
        names.remove(key)  # Remove keys not present in this file
    elements = list(root)
    elements.sort(key=lambda elem: names.index(elem.get("name")))
    root.clear()
    for elem in elements:
        root.append(elem)
    tree.write(input_path, encoding='utf-8', xml_declaration=True)
    print(f'Sorted strings written to {input_path.relative_to(ROOT_PATH)}')


def main():
    for res_path in ROOT_PATH.rglob("res"):
        names = get_strings_names(res_path / "values/strings.xml")
        for values_dir in res_path.glob("values-*/strings.xml"):
            sort_strings_xml(values_dir, names)


if __name__ == '__main__':
    main()
