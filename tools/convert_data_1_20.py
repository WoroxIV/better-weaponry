"""
Convertit les fichiers de données du mod (format 1.21.1) au format 1.20.1.

Les données sont générées sur la branche mc/1.21.1. Après les avoir recopiées ici (common/src/generated/resources et
common/src/main/resources/data), relancer ce script depuis la racine du projet : python tools/convert_data_1_20.py
Un fichier déjà converti n'est pas modifié.
"""
import json
import pathlib
import shutil

ROOTS = [pathlib.Path("common/src/generated/resources/data"), pathlib.Path("common/src/main/resources/data")]

# Dossiers renommés entre les deux versions.
FOLDERS = {"recipe": "recipes", "advancement": "advancements", "structure": "structures", "loot_table": "loot_tables"}
TAG_FOLDERS = {"item": "items", "entity_type": "entity_types", "block": "blocks"}

# Ces enchantements sont des classes en 1.20.1, et l'enchantabilité y passe par les catégories d'enchantement.
REMOVED = ("better_weaponry/enchantment", "minecraft/tags/enchantment")
REMOVED_TAG_FOLDERS = ("enchantable",)

CONDITIONS = {"neoforge:mod_loaded": "forge:mod_loaded", "neoforge:not": "forge:not", "neoforge:or": "forge:or",
              "neoforge:tag_empty": "forge:tag_empty", "neoforge:false": "forge:false"}


def forge_tag(tag):
    """Les tags communs "c:" de la 1.21 sont les tags "forge:" de la 1.20."""
    prefix = "#" if tag.startswith("#") else ""
    name = tag[len(prefix):]
    if not name.startswith("c:"):
        return tag
    path = name[2:]
    renamed = {"strings": "string", "tools/shield": "tools/shields", "tools/bow": "tools/bows", "tools/crossbow": "tools/crossbows"}.get(path, path)
    return f"{prefix}forge:{renamed}"


def convert_condition(condition):
    converted = dict(condition)
    converted["type"] = CONDITIONS[condition["type"]]
    if "value" in converted:
        converted["value"] = convert_condition(converted["value"])
    if "values" in converted:
        converted["values"] = [convert_condition(value) for value in converted["values"]]
    if "tag" in converted:
        converted["tag"] = forge_tag(converted["tag"])
    return converted


def convert_ingredients(node):
    """Remplace les tags communs partout dans une recette."""
    if isinstance(node, dict):
        return {key: forge_tag(value) if key == "tag" and isinstance(value, str) else convert_ingredients(value) for key, value in node.items()}
    if isinstance(node, list):
        return [convert_ingredients(value) for value in node]
    return node


def convert_recipe(json_data):
    result = {}
    if "neoforge:conditions" in json_data:
        result["conditions"] = [convert_condition(condition) for condition in json_data["neoforge:conditions"]]
    for key, value in json_data.items():
        if key == "neoforge:conditions":
            continue
        if key == "result" and isinstance(value, dict) and "id" in value:
            output = {"item": value["id"]}
            if value.get("count", 1) != 1:
                output["count"] = value["count"]
            result["result"] = output
        else:
            result[key] = convert_ingredients(value)
    return result


def convert_item_predicate(predicate):
    items = predicate.get("items")
    if isinstance(items, str):
        predicate = dict(predicate)
        del predicate["items"]
        if items.startswith("#"):
            predicate["tag"] = forge_tag(items)[1:]
        else:
            predicate["items"] = [items]
    return predicate


def convert_advancement(json_data):
    result = {}
    if "neoforge:conditions" in json_data:
        result["conditions"] = [convert_condition(condition) for condition in json_data["neoforge:conditions"]]
    for key, value in json_data.items():
        if key != "neoforge:conditions":
            result[key] = value
    for criterion in result.get("criteria", {}).values():
        conditions = criterion.get("conditions", {})
        if isinstance(conditions.get("items"), list):
            conditions["items"] = [convert_item_predicate(predicate) for predicate in conditions["items"]]
    return result


def convert_tag(json_data):
    entries = []
    for entry in json_data.get("values", []):
        if isinstance(entry, dict):
            entries.append({**entry, "id": forge_tag(entry["id"])})
        else:
            entries.append(forge_tag(entry))
    return {**json_data, "values": entries}


def write(path, data):
    path.write_bytes((json.dumps(data, indent=2, ensure_ascii=False) + "\n").encode("utf-8"))


def relocate(root):
    """Renomme les dossiers, du plus profond au moins profond."""
    for directory in sorted((p for p in root.rglob("*") if p.is_dir()), key=lambda p: len(p.parts), reverse=True):
        relative = directory.relative_to(root).as_posix()
        if any(relative == removed or relative.endswith("/" + removed) for removed in REMOVED):
            shutil.rmtree(directory)
            continue
        if directory.name in REMOVED_TAG_FOLDERS and directory.parent.name in ("items", "item"):
            shutil.rmtree(directory)
            continue
        target = None
        if directory.parent.name == "tags" and directory.name in TAG_FOLDERS:
            target = directory.with_name(TAG_FOLDERS[directory.name])
        elif directory.name in FOLDERS and directory.parent.parent == root:
            target = directory.with_name(FOLDERS[directory.name])
        if target is not None:
            if target.exists():
                for child in directory.iterdir():
                    child.replace(target / child.name)
                directory.rmdir()
            else:
                directory.rename(target)

    # Tags communs : data/c/tags/items/... devient data/forge/tags/items/...
    common = root / "c"
    if common.is_dir():
        for file in sorted(p for p in common.rglob("*.json")):
            relative = file.relative_to(common).as_posix()
            name = relative[len("tags/items/"):-len(".json")]
            target = root / "forge" / "tags" / "items" / (forge_tag("c:" + name)[len("forge:"):] + ".json")
            target.parent.mkdir(parents=True, exist_ok=True)
            file.replace(target)
        shutil.rmtree(common)


def convert(root):
    for file in root.rglob("*.json"):
        relative = file.relative_to(root).parts
        data = json.loads(file.read_text(encoding="utf-8"))
        if len(relative) > 1 and relative[1] == "recipes":
            write(file, convert_recipe(data))
        elif len(relative) > 1 and relative[1] == "advancements":
            write(file, convert_advancement(data))
        elif len(relative) > 2 and relative[1] == "tags":
            write(file, convert_tag(data))


for data_root in ROOTS:
    relocate(data_root)
    convert(data_root)

leftovers = [str(p) for root in ROOTS for p in root.rglob("*.json") if "neoforge:conditions" in p.read_text(encoding="utf-8") or '"c:' in p.read_text(encoding="utf-8")]
print("converted; files still using 1.21 notation:", len(leftovers), leftovers[:5])
