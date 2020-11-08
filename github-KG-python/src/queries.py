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
query {
  repository(owner: "%s", name: "%s") {
    createdAt
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
    forkCount
    homepageUrl
    isDisabled
    isEmpty
    isFork
    isInOrganization
    isLocked
    isMirror
    isPrivate
    isTemplate
    issues(first: 1) {
      totalCount
    }
    isUserConfigurationRepository
    languages (first: 100) {
      totalCount
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
    name
    nameWithOwner
    owner {
      login
    }
    primaryLanguage {
      name
    }
    pullRequests(first: 1) {
      totalCount
    }
    pushedAt
    repositoryTopics(first: 10) {
      totalCount
      pageInfo {
        endCursor
        hasNextPage
      }
      nodes {
        topic {
          name
          relatedTopics {
            name
          }
          stargazerCount
        }
      }
    }
    stargazerCount
    updatedAt
    url
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