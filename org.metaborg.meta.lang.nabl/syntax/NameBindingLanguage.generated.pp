[
   Namespaces                                             -- V  [H  [KW["namespaces"]] _1],
   Namespaces.1:iter-star                                 -- _1,
   NamespaceDef                                           -- _1,
   NamespaceRef                                           -- _1,
   COMPLETION-ModuleSection                               -- _1,
   COMPLETION-NamespaceDef                                -- _1,
   COMPLETION-NamespaceRef                                -- _1,
   Properties                                             -- V  [H  [KW["properties"]] _1],
   Properties.1:iter-star                                 -- _1,
   PropertyDef                                            -- _1 KW["of"] _2 KW[":"] _3,
   PropertyDef.2:iter-sep                                 -- _1 KW[","],
   TypeProp                                               -- KW["type"],
   PropertyRef                                            -- _1,
   PropertyTerm                                           -- KW["of"] _1 _2,
   PropertyPattern                                        -- KW["of"] _1 _2 _3,
   Equal                                                  -- ,
   Conformant                                             -- KW["conformant"],
   PropertyConstraint                                     -- KW["where"] _1 KW["has"] _2 _3,
   COMPLETION-ModuleSection                               -- _1,
   COMPLETION-PropertyDef                                 -- _1,
   COMPLETION-PropertyRef                                 -- _1,
   COMPLETION-PropertyTerm                                -- _1,
   COMPLETION-PropertyPattern                             -- _1,
   COMPLETION-PropFilter                                  -- _1,
   COMPLETION-Constraint                                  -- _1,
   BindingRules                                           -- V  [H  [KW["binding"] KW["rules"]] _1],
   BindingRules.1:iter-star                               -- _1,
   BindingRule                                            -- _1 _2 KW[":"] _3,
   BindingRule.2:iter-star                                -- _1,
   BindingRule.3:iter                                     -- _1,
   COMPLETION-ModuleSection                               -- _1,
   COMPLETION-BindingRule                                 -- _1,
   DefClause                                              -- _1 KW["defines"] _2 _3 _4 _5 _6 _7,
   DefClause.5:iter-star                                  -- _1,
   DefClause.7:iter-star                                  -- _1,
   Explicit                                               -- ,
   Implicit                                               -- KW["implicitly"],
   Unique                                                 -- ,
   Unique                                                 -- KW["unique"],
   NonUnique                                              -- KW["non-unique"],
   ScopeClause                                            -- V  [H  [KW["scopes"]] _1],
   ScopeClause.1:iter-sep                                 -- _1 KW[","],
   RefClause                                              -- _1,
   RefClause.1:iter-sep                                   -- _1 KW["otherwise"],
   RefClausePart                                          -- KW["refers"] KW["to"] _1 _2 _3 _4 _5 _6,
   RefClausePart.4:iter-star                              -- _1,
   RefClausePart.6:iter-star                              -- _1,
   ImportClause                                           -- _1,
   ImportClause.1:iter-sep                                -- _1 KW["otherwise"],
   SingleImport                                           -- KW["imports"] _1 _2 _3 _4 _5 _6 _7 _8,
   SingleImport.4:iter-star                               -- _1,
   SingleImport.8:iter-star                               -- _1,
   WildcardImport                                         -- KW["imports"] _1 _2 _3 _4 _5,
   WildcardImport.1:iter-sep                              -- _1 KW[","],
   WildcardImport.2:iter-star                             -- _1,
   WildcardImport.5:iter-star                             -- _1,
   None                                                   -- ,
   Alias                                                  -- KW["as"] _1,
   Direct                                                 -- _1,
   Transitive                                             -- KW["imported"] _1,
   COMPLETION-DefKind                                     -- _1,
   COMPLETION-Unique                                      -- _1,
   COMPLETION-RefClausePart                               -- _1,
   COMPLETION-BindingClause                               -- _1,
   COMPLETION-ImportClausePart                            -- _1,
   COMPLETION-Alias                                       -- _1,
   COMPLETION-ImportRef                                   -- _1,
   Current                                                -- ,
   Current                                                -- ,
   Current                                                -- KW["current"] KW["scope"],
   DefScopes                                              -- _1,
   DefScopes.1:iter-sep                                   -- _1 KW[","],
   Subsequent                                             -- KW["subsequent"] KW["scope"],
   DefScope                                               -- _1,
   Current                                                -- ,
   Current                                                -- ,
   Current                                                -- KW["current"] KW["scope"],
   Enclosing                                              -- KW["enclosing"] _1,
   Context                                                -- _1 _2 _3 _4 _5,
   Context.4:iter-star                                    -- _1,
   RefScope                                               -- _1,
   All                                                    -- ,
   Best                                                   -- KW["best"],
   COMPLETION-InDefScopes                                 -- _1,
   COMPLETION-IntoDefScopes                               -- _1,
   COMPLETION-DefScopes                                   -- _1,
   COMPLETION-DefScope                                    -- _1,
   COMPLETION-InRefScope                                  -- _1,
   COMPLETION-FromRefScope                                -- _1,
   COMPLETION-RefScope                                    -- _1,
   COMPLETION-Disambiguator                               -- _1,
   ReferenceConstraint                                    -- KW["where"] _1 KW["refers"] KW["to"] _2 _3 _4 _5,
   ReferenceConstraint.4:iter-star                        -- _1,
   COMPLETION-Constraint                                  -- _1,
   MessageClause                                          -- _1 _2 KW["on"] _3 _4,
   MessageClause.4:iter-star                              -- _1,
   Note                                                   -- KW["note"],
   Warning                                                -- KW["warning"],
   Error                                                  -- KW["error"],
   COMPLETION-BindingClause                               -- _1,
   COMPLETION-MessageKind                                 -- _1,
   Module                                                 -- KW["module"] _1 _2 _3,
   Module.2:iter-star                                     -- _1,
   Module.3:iter-star                                     -- _1,
   Imports                                                -- V  [H  [KW["imports"]] _1],
   Imports.1:iter-star                                    -- _1,
   ImportWildcard                                         -- _1 KW["/-"],
   Import                                                 -- _1,
   COMPLETION-Module                                      -- _1,
   COMPLETION-ModuleSection                               -- _1,
   COMPLETION-ImportModule                                -- _1,
   Todo                                                   -- KW["todo"] _1 _2,
   Fixme                                                  -- KW["fixme"] _1 _2,
   Discuss                                                -- KW["discuss"] _1 _2,
   TestSuite                                              -- KW["test"] KW["suite"] _1,
   AssignedTo                                             -- _1,
   AssignedTo.1:iter-star-sep                             -- _1 KW[","],
   AssignedTo.1:iter-star-sep.1:iter                      -- _1,
   Example                                                -- KW["example"] _1,
   Description                                            -- KW["description"] _1,
   Version                                                -- KW["version"] _1,
   Status                                                 -- V  [H  [KW["status"]] _1],
   Status.1:iter                                          -- _1,
   Author                                                 -- KW["author"] _1 _2 _3,
   Author.1:iter                                          -- _1,
   None                                                   -- ,
   EMail                                                  -- KW["("] _1 KW[")"],
   None                                                   -- ,
   Affiliation                                            -- V  [H  [KW[","]] _1],
   Affiliation.1:iter                                     -- _1,
   SortVar                                                -- _1,
   SortNoArgs                                             -- _1,
   Sort                                                   -- _1 KW["("] _2 KW[")"],
   Sort.2:iter-star-sep                                   -- _1 KW[","],
   TypeDecl                                               -- _1 _2 _3,
   TypeDeclQ                                              -- _1 _2 _3,
   Str                                                    -- _1,
   NoTypeParams                                           -- ,
   TypeParams                                             -- KW["("] _1 KW[")"],
   TypeParams.1:iter-sep                                  -- _1 KW[","],
   TypeParam                                              -- _1 KW[":"] _2,
   Int                                                    -- _1,
   Real                                                   -- _1,
   Str                                                    -- _1,
   Op                                                     -- _1 KW["("] _2 KW[")"],
   Op.2:iter-star-sep                                     -- _1 KW[","],
   Op.2:iter-star-sep.1:parameterized-sort                -- _1 _2,
   Op.2:iter-star-sep.1:parameterized-sort.1:"Term"       -- ,
   OpQ                                                    -- _1 KW["("] _2 KW[")"],
   OpQ.2:iter-star-sep                                    -- _1 KW[","],
   OpQ.2:iter-star-sep.1:parameterized-sort               -- _1 _2,
   OpQ.2:iter-star-sep.1:parameterized-sort.1:"Term"      -- ,
   NoAnnoList                                             -- _1,
   NoAnnoList.1:parameterized-sort                        -- _1 _2,
   NoAnnoList.1:parameterized-sort.1:"PreTerm"            -- ,
   Char                                                   -- _1,
   Tuple                                                  -- KW["("] _1 KW[")"],
   Tuple.1:iter-star-sep                                  -- _1 KW[","],
   Tuple.1:iter-star-sep.1:parameterized-sort             -- _1 _2,
   Tuple.1:iter-star-sep.1:parameterized-sort.1:"Term"    -- ,
   List                                                   -- KW["["] _1 KW["]"],
   List.1:iter-star-sep                                   -- _1 KW[","],
   List.1:iter-star-sep.1:parameterized-sort              -- _1 _2,
   List.1:iter-star-sep.1:parameterized-sort.1:"Term"     -- ,
   ListTail                                               -- KW["["] _1 KW["|"] _2 KW["]"],
   ListTail.1:iter-star-sep                               -- _1 KW[","],
   ListTail.1:iter-star-sep.1:parameterized-sort          -- _1 _2,
   ListTail.1:iter-star-sep.1:parameterized-sort.1:"Term" -- ,
   ListTail.2:parameterized-sort                          -- _1 _2,
   ListTail.2:parameterized-sort.1:"Term"                 -- ,
   Wld                                                    -- KW["_"],
   ListWld                                                -- KW["..."],
   Var                                                    -- _1,
   ListVar                                                -- _1,
   VarRef                                                 -- _1,
   ListVarRef                                             -- _1
]