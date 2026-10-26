"""Publication regressions for intentional repeated checks and unexpected duplicates."""
import unittest
import xml.etree.ElementTree as ET
from name_repeated_checks import CLASS, NAME, normalize


class ReportIdentityTest(unittest.TestCase):
    def test_repeats_keep_both_outcomes_and_durations(self):
        data = f'<testsuite tests="2" failures="1"><testcase classname="{CLASS}" name="{NAME}" time="1"/><testcase classname="{CLASS}" name="{NAME}" time="2"><failure message="bad"/></testcase></testsuite>'.encode()
        root = ET.fromstring(normalize(data))
        cases = list(root)
        self.assertEqual(root.attrib, {'tests': '2', 'failures': '1'})
        self.assertEqual([c.get('name') for c in cases], [f'{NAME} [invocation 1 of 2]', f'{NAME} [invocation 2 of 2]'])
        self.assertEqual([c.get('time') for c in cases], ['1', '2'])
        self.assertEqual(cases[1].find('failure').get('message'), 'bad')

    def test_unexpected_duplicate_fails(self):
        with self.assertRaises(ValueError):
            normalize(b'<testsuite><testcase classname="x" name="y"/><testcase classname="x" name="y"/></testsuite>')

    def test_wrong_invocation_count_fails(self):
        case = f'<testcase classname="{CLASS}" name="{NAME}"/>'
        with self.assertRaises(ValueError):
            normalize(('<testsuite>'+case*3+'</testsuite>').encode())

    def test_unique_report_is_idempotent(self):
        data = b'<testsuite><testcase classname="x" name="y" time="4"><skipped/></testcase></testsuite>'
        self.assertEqual(normalize(normalize(data)), normalize(data))
