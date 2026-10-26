"""Give the two deliberate TestNG health-check invocations distinct JUnit identities.

Surefire otherwise writes identical class/name pairs for invocationCount=2. Only this
known example is renamed; unexpected duplicates fail publication rather than being hidden.
Original outcome, time, failure, retry and count fields are preserved in a separate copy.
"""
from collections import Counter
from pathlib import Path
import xml.etree.ElementTree as ET

CLASS = 'com.automation.playwright.tests.interview.TestNgFeaturesTest'
NAME = 'healthCheckAnswersEveryTime'


def normalize(data: bytes) -> bytes:
    root = ET.fromstring(data)
    cases = list(root.iter('testcase'))
    counts = Counter((c.get('classname'), c.get('name')) for c in cases)
    for identity, count in counts.items():
        if count > 1 and (identity != (CLASS, NAME) or count != 2):
            raise ValueError(f'Unexpected duplicate JUnit identity: {identity}, count={count}')
    repeated = [c for c in cases if (c.get('classname'), c.get('name')) == (CLASS, NAME)]
    if len(repeated) == 2:
        for number, case in enumerate(repeated, 1):
            case.set('name', f'{NAME} [invocation {number} of 2]')
    return ET.tostring(root, encoding='utf-8', xml_declaration=True)


if __name__ == '__main__':
    source = Path('target/surefire-reports')
    output = Path('target/collection-junit')
    output.mkdir(parents=True, exist_ok=True)
    reports = sorted(source.glob('TEST-*.xml'))
    if not reports:
        raise SystemExit('No JUnit reports available; nothing to publish')
    for report in reports:
        (output / report.name).write_bytes(normalize(report.read_bytes()))
    print(f'Prepared {len(reports)} JUnit reports; original Surefire files unchanged')
