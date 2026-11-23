"""Merges the failsafe reports of the hidden suite into one JUnit file.

Each test case is named "<rule id> <category> <text>" (hidden-evaluation.md). When the build did
not name cases by their display name, the method name (e.g. r003_06_boundary) is converted.
When no report exists (the suite did not compile or start), one failing case says so.
Exit code: 0 when every case passed, else 1.
"""

import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

CATEGORIES = "boundary|combination|invalid|role|other"


def display_name(name: str) -> str:
    if re.match(r"^(R-\d{3}-\d{2}|BR-\d{2}) ", name):
        return name
    method = re.match(rf"^r(\d{{3}})_(\d{{2}})_({CATEGORIES})", name)
    if method:
        return f"R-{method.group(1)}-{method.group(2)} {method.group(3)} ({name})"
    shared = re.match(rf"^br(\d{{2}})_({CATEGORIES})", name)
    if shared:
        return f"BR-{shared.group(1)} {shared.group(2)} ({name})"
    return name


def main(report_dir: str, out_file: str) -> int:
    suite = ET.Element("testsuite", name="hidden")
    tests = failures = errors = 0
    for path in sorted(glob.glob(os.path.join(report_dir, "TEST-*.xml"))):
        for case in ET.parse(path).getroot().iter("testcase"):
            case.set("name", display_name(case.get("name", "")))
            for noisy in ("system-out", "system-err"):
                for child in case.findall(noisy):
                    case.remove(child)
            tests += 1
            failures += len(case.findall("failure"))
            errors += len(case.findall("error"))
            suite.append(case)
    if tests == 0:
        case = ET.SubElement(suite, "testcase", name="hidden suite did not run", classname="hidden")
        ET.SubElement(case, "error", message="no failsafe report; see target/maven.log")
        tests = errors = 1
    suite.set("tests", str(tests))
    suite.set("failures", str(failures))
    suite.set("errors", str(errors))
    root = ET.Element("testsuites")
    root.append(suite)
    os.makedirs(os.path.dirname(out_file), exist_ok=True)
    ET.ElementTree(root).write(out_file, encoding="utf-8", xml_declaration=True)
    return 0 if failures + errors == 0 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
