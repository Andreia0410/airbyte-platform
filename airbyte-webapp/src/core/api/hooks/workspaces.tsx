import { QueryObserverResult, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useLayoutEffect } from "react";

import { useCurrentWorkspaceId } from "area/workspace/utils";
import { SCOPE_USER, SCOPE_WORKSPACE } from "services/Scope";

import {
  createWorkspace,
  deleteWorkspace,
  getWorkspace,
  listWorkspaces,
  updateWorkspace,
  updateWorkspaceName,
  webBackendGetWorkspaceState,
} from "../generated/AirbyteClient";
import { WorkspaceReadList, WorkspaceUpdate, WorkspaceUpdateName } from "../types/AirbyteClient";
import { useRequestOptions } from "../useRequestOptions";
import { useSuspenseQuery } from "../useSuspenseQuery";

export const workspaceKeys = {
  all: [SCOPE_USER, "workspaces"] as const,
  lists: () => [...workspaceKeys.all, "list"] as const,
  list: (filters: string) => [...workspaceKeys.lists(), { filters }] as const,
  detail: (workspaceId: string) => [...workspaceKeys.all, "details", workspaceId] as const,
  state: (workspaceId: string) => [...workspaceKeys.all, "state", workspaceId] as const,
};

export const useCurrentWorkspace = () => {
  const workspaceId = useCurrentWorkspaceId();

  return useGetWorkspace(workspaceId, {
    staleTime: Infinity,
  });
};

export const getCurrentWorkspaceStateQueryKey = (workspaceId: string) => {
  return workspaceKeys.state(workspaceId);
};

export const useCreateWorkspace = () => {
  const requestOptions = useRequestOptions();
  const queryClient = useQueryClient();

  return useMutation(async (name: string) => createWorkspace({ name }, requestOptions), {
    onSuccess: (result) => {
      queryClient.setQueryData<WorkspaceReadList>(workspaceKeys.lists(), (old) => ({
        workspaces: [result, ...(old?.workspaces ?? [])],
      }));
    },
  });
};

export const useDeleteCurrentWorkspace = () => {
  const requestOptions = useRequestOptions();
  const queryClient = useQueryClient();

  return useMutation(async (workspaceId: string) => deleteWorkspace({ workspaceId }, requestOptions), {
    onSuccess: (_, workspaceId) => {
      queryClient.setQueryData<WorkspaceReadList>(workspaceKeys.lists(), (old) =>
        old ? { workspaces: old.workspaces.filter((workspace) => workspace.workspaceId !== workspaceId) } : undefined
      );
    },
  });
};

export const useGetCurrentWorkspaceStateQuery = (workspaceId: string) => {
  const requestOptions = useRequestOptions();
  return () => webBackendGetWorkspaceState({ workspaceId }, requestOptions);
};

export const useCurrentWorkspaceState = () => {
  const workspaceId = useCurrentWorkspaceId();
  const queryKey = getCurrentWorkspaceStateQueryKey(workspaceId);
  const queryFn = useGetCurrentWorkspaceStateQuery(workspaceId);

  return useSuspenseQuery(queryKey, queryFn, {
    // We want to keep this query only shortly in cache, so we refetch
    // the data whenever the user might have changed sources/destinations/connections
    // without requiring to manually invalidate that query on each change.
    cacheTime: 5 * 1000,
  });
};

export const useInvalidateWorkspaceStateQuery = () => {
  const queryClient = useQueryClient();
  const workspaceId = useCurrentWorkspaceId();
  return useCallback(() => {
    queryClient.invalidateQueries(workspaceKeys.state(workspaceId));
  }, [queryClient, workspaceId]);
};

// todo: after merging https://github.com/airbytehq/airbyte-platform-internal/pull/7779 this should get workspace by user id
export const useListWorkspaces = () => {
  const requestOptions = useRequestOptions();
  return useSuspenseQuery(workspaceKeys.lists(), () => listWorkspaces(requestOptions));
};

export const getListWorkspacesAsyncQueryKey = () => {
  return workspaceKeys.lists();
};

export const useListWorkspacesAsyncQuery = () => {
  const requestOptions = useRequestOptions();

  return () => listWorkspaces(requestOptions);
};

export function useListWorkspacesAsync(): QueryObserverResult<WorkspaceReadList> {
  const queryKey = getListWorkspacesAsyncQueryKey();
  const queryFn = useListWorkspacesAsyncQuery();

  return useQuery(queryKey, queryFn);
}

export const getWorkspaceQueryKey = (workspaceId: string) => {
  return workspaceKeys.detail(workspaceId);
};

export const useGetWorkspaceQuery = (workspaceId: string) => {
  const requestOptions = useRequestOptions();
  return () => getWorkspace({ workspaceId }, requestOptions);
};

export const useGetWorkspace = (
  workspaceId: string,
  options?: {
    staleTime: number;
  }
) => {
  const queryKey = getWorkspaceQueryKey(workspaceId);
  const queryFn = useGetWorkspaceQuery(workspaceId);

  return useSuspenseQuery(queryKey, queryFn, options);
};

export const useUpdateWorkspace = () => {
  const requestOptions = useRequestOptions();
  const queryClient = useQueryClient();

  return useMutation((workspace: WorkspaceUpdate) => updateWorkspace(workspace, requestOptions), {
    onSuccess: (data) => {
      queryClient.setQueryData(workspaceKeys.detail(data.workspaceId), data);
    },
  });
};

export const useUpdateWorkspaceName = () => {
  const requestOptions = useRequestOptions();
  const queryClient = useQueryClient();

  return useMutation(
    (workspaceUpdateNameBody: WorkspaceUpdateName) => updateWorkspaceName(workspaceUpdateNameBody, requestOptions),
    {
      onSuccess: (data) => {
        queryClient.setQueryData(workspaceKeys.detail(data.workspaceId), data);
      },
    }
  );
};

export const useInvalidateWorkspace = (workspaceId: string) => {
  const queryClient = useQueryClient();

  return useCallback(
    () => queryClient.invalidateQueries(workspaceKeys.detail(workspaceId)),
    [queryClient, workspaceId]
  );
};

export const useInvalidateAllWorkspaceScope = () => {
  const queryClient = useQueryClient();

  return useCallback(() => {
    queryClient.removeQueries([SCOPE_WORKSPACE]);
  }, [queryClient]);
};

export const useInvalidateAllWorkspaceScopeOnChange = (workspaceId: string) => {
  const invalidateWorkspaceScope = useInvalidateAllWorkspaceScope();
  // useLayoutEffect so the query removal happens before the new workspace's queries are triggered
  useLayoutEffect(() => {
    invalidateWorkspaceScope();
  }, [invalidateWorkspaceScope, workspaceId]);
};
