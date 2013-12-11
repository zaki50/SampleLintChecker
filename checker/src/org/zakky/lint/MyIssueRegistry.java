/*
 * Copyright(C) 2013 TOYOTA InfoTechnology Center Co.,LTD. All Rights Reserved.
 */

package org.zakky.lint;

import java.util.Arrays;
import java.util.List;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;

public class MyIssueRegistry extends IssueRegistry {
    public MyIssueRegistry() {
    }

    @Override
    public List<Issue> getIssues() {
        return Arrays.asList(//
                PrngFixDetector.ISSUE
                );
    }

}
