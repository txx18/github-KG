// 建立索引
create index repo_name_index for (n:Repo) on (n.nameWithOwner);
create index owner_login_index for (n:Owner) on (n.login);
create index language_name_index for (n:Language) on (n.name);
create index topic_name_index for (n:Topic) on (n.name);
create index package_name_index for (n:Package) on (n.packageName);

create index uses_index for (r:USES) on (r.size);