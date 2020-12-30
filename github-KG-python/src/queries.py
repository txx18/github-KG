topic_query = """
query{
  topic(name: "%s") {
    name
    relatedTopics(first: 10) {
      name
    }
    stargazerCount
  }
}
"""

repos_query = """
{
  repository(owner: "%s", name: "%s") {
    assignableUsers {
      totalCount
    }
    commitComments {
      totalCount
    }
    createdAt
    databaseId
    deleteBranchOnMerge
    dependencyGraphManifests(first: 100) {
      totalCount
      pageInfo {
        endCursor
        hasNextPage
      }
      nodes {
        blobPath
        dependencies(first: 100) {
          totalCount
          pageInfo {
            endCursor
            hasNextPage
          }
          nodes {
            hasDependencies
            packageManager
            packageName
            repository {
              nameWithOwner
            }
            requirements
          }
        }
        dependenciesCount
        exceedsMaxSize
        filename
        parseable
      }
    }
    description
    diskUsage
    forkCount
    forks {
      totalCount
    }
    hasIssuesEnabled
    hasProjectsEnabled
    hasWikiEnabled
    homepageUrl
    isArchived
    isBlankIssuesEnabled
    isDisabled
    isEmpty
    isFork
    isInOrganization
    isLocked
    isMirror
    isPrivate
    isSecurityPolicyEnabled
    isTemplate
    issues(first: 1) {
      totalCount
    }
    isUserConfigurationRepository
    labels {
      totalCount
    }
    languages(first: 100) {
      totalCount
      totalSize
      pageInfo {
        endCursor
        hasNextPage
      }
      edges {
        size
      }
      nodes {
        name
      }
    }
    licenseInfo {
      name
    }
    mentionableUsers {
      totalCount
    }
    mergeCommitAllowed
    milestones {
      totalCount
    }
    mirrorUrl
    name
    nameWithOwner
    openGraphImageUrl
    owner {
      login
    }
    packages {
      totalCount
    }
    parent {
      nameWithOwner
    }
    primaryLanguage {
      name
    }
    projects {
      totalCount
    }
    projectsResourcePath
    projectsUrl
    pullRequests {
      totalCount
    }
    pushedAt
    rebaseMergeAllowed
    releases {
      totalCount
    }
    repositoryTopics(first: 10) {
      totalCount
      pageInfo {
        endCursor
        hasNextPage
      }
      nodes {
        topic {
          name
          relatedTopics(first: 3) {
            name
          }
        }
      }
    }
    resourcePath
    securityPolicyUrl
    squashMergeAllowed
    sshUrl
    stargazerCount
    stargazers {
      totalCount
    }
    submodules {
      totalCount
    }
    templateRepository {
      nameWithOwner
    }
    updatedAt
    url
    vulnerabilityAlerts {
      totalCount
    }
    watchers {
      totalCount
    }
  }
}
"""

rateLimit_query = """
query {
  viewer {
    login
  }
  rateLimit {
    limit
    cost
    remaining
    resetAt
  }
}
"""
