/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.spacceleratorsamplesaddon.setup.impl;

import de.hybris.platform.addonsupport.setup.impl.DefaultAddonSampleDataImportService;
import de.hybris.platform.catalog.jalo.SyncItemCronJob;
import de.hybris.platform.catalog.jalo.SyncItemJob;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.catalog.model.SyncItemJobModel;
import de.hybris.platform.core.initialization.SystemSetupContext;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.List;

import org.springframework.beans.factory.annotation.Required;


/**
 * This class extends {@link DefaultAddonSampleDataImportService} and specifies how to import sample data spartacus
 */
@SuppressWarnings("deprecation")
public class SpaSampleAddOnSampleDataImportService extends DefaultAddonSampleDataImportService
{
	private static final String SYNC_CONTENT_CATALOG = "electronics->spa";

	private ModelService modelService;

	@Override
	protected void importContentCatalog(final SystemSetupContext context, final String importRoot, final String catalogName)
	{
		// 1- create new catalog
		importImpexFile(context, importRoot + "/contentCatalogs/" + catalogName + "ContentCatalog/catalog.impex", false);

		// 2- sync electronicsContentCatalog:Staged->electronics-spaContentCatalog:Staged
		final CatalogVersionModel catalog = getCatalogVersionService().getCatalogVersion("electronics-spaContentCatalog", "Staged");
		List<SyncItemJobModel> synItemsJobs = catalog.getIncomingSynchronizations();
		if (synItemsJobs.size() > 0)
		{
			SyncItemJobModel job = synItemsJobs.get(0);
			final SyncItemJob jobItem = getModelService().getSource(job);
			synchronizeSpaContentCatalog(context, jobItem);
		}

		// 3- import content catalog from impex
		super.importContentCatalog(context, importRoot, catalogName);

		// 4- synchronize spaContentCatalog:staged->online
		synchronizeContentCatalog(context, "electronics-spa", true);
		
		// 5- give permission to cmsmanager to do the sync
		importImpexFile(context, importRoot + "/contentCatalogs/" + catalogName + "ContentCatalog/sync.impex", false);
	}

	private void synchronizeSpaContentCatalog(final SystemSetupContext context, SyncItemJob syncJobItem)
	{
		logInfo(context, "Begin synchronizing Content Catalog [" + SYNC_CONTENT_CATALOG + "] - synchronizing");

		final SyncItemCronJob syncCronJob = syncJobItem.newExecution();
		syncCronJob.setLogToDatabase(false);
		syncCronJob.setLogToFile(false);
		syncCronJob.setForceUpdate(false);
		syncJobItem.configureFullVersionSync(syncCronJob);

		logInfo(context, "Starting synchronization, this may take a while ...");
		syncJobItem.perform(syncCronJob, true);

		logInfo(context, "Synchronization complete for catalog [" + SYNC_CONTENT_CATALOG + "]");
		final CronJobResult result = modelService.get(syncCronJob.getResult());
		final CronJobStatus status = modelService.get(syncCronJob.getStatus());

		PerformResult syncCronJobResult = new PerformResult(result, status);
		if (isSyncRerunNeeded(syncCronJobResult))
		{
			logInfo(context, "Catalog catalog [" + SYNC_CONTENT_CATALOG + "] sync has issues.");
		}

		logInfo(context, "Done synchronizing  Content Catalog [" + SYNC_CONTENT_CATALOG + "]");
	}

	protected ModelService getModelService()
	{
		return modelService;
	}

	@Required
	public void setModelService(ModelService modelService)
	{
		this.modelService = modelService;
	}
}
